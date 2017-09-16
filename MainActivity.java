package com.example.nurdaulet.naimi;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Camera;
import android.media.MediaCodec;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.VideoProfile;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.mp4parser.Container;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.Track;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.container.mp4.MovieCreator;
import org.mp4parser.muxer.tracks.AppendTrack;
import org.mp4parser.muxer.tracks.TextTrackImpl;
import org.mp4parser.muxer.tracks.h265.VideoParameterSet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String mainDirectory = "/NaimiVideos";

    /**
     * The {@link ArrayList<String>} will collect interview questions.
     */
    private ArrayList<String> questions = new ArrayList<>();
    private int id = 1;
    private TextView timerTextview;
    private TextView questionTextview;
    private MediaMetadataRetriever retriever;
    private boolean mContentLoaded;
    private long startTime;
    private long endTime;

    /**
     * The view (or view group) containing the content. This is one of two overlapping views.
     */
    private View mContentView;

    /**
     * The view containing the loading indicator. This is the other of two overlapping views.
     */
    private View mLoadingView;

    /**
     * The system "short" animation time duration, in milliseconds. This duration is ideal for
     * subtle animations or animations that occur very frequently.
     */
    private int mShortAnimationDuration;

    private File root;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContentView = findViewById(R.id.contentView);
        mLoadingView = findViewById(R.id.loading_spinner);
        mLoadingView.setVisibility(View.GONE);

        // Retrieve and cache the system's default "short" animation time.
        mShortAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);

        //collecting questions
        questions.add("Ваше полное имя, должность и опыт работы");
      questions.add("Чем Вас привлекает работа у нас в данной должности?");
  /*      questions.add("Каковы Ваши сильные стороны?");
        questions.add("Каковы Ваши слабые стороны?");
        questions.add("Почему Вы считаете себя достойным занять эту должность? В чем Ваши преимущества перед другими кандидатами?");
        questions.add("Получали ли Вы другие предложения работы?");*/

        final Button start = (Button) findViewById(R.id.start);
        timerTextview = (TextView) findViewById(R.id.timer);
        questionTextview = (TextView) findViewById(R.id.question);
        questionTextview.setText(questions.get(id - 1));
        retriever = new MediaMetadataRetriever();
        root = Environment.getExternalStorageDirectory();

        deleteVideos();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                questionTextview.setVisibility(View.VISIBLE);
                start();
           /*     LongOperation lp = new LongOperation(getApplicationContext());
                lp.execute(null, null, null);*/
                start.setVisibility(View.GONE);
            }
        });

/*        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mergeVideo();
                start.setVisibility(View.GONE);
            }
        });*/
    }

    private void deleteVideos() {

        File mainDir = new File(root, mainDirectory);
        if (!mainDir.exists()) {
            try {
                if (!mainDir.mkdirs()) throw new Exception("Can not create directory");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        for (File f : mainDir.listFiles()) {
            if (f.isFile()) {
                f.delete();
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (id != questions.size() + 1) {
                start();
            } else {
                LongOperation lp = new LongOperation(this);
                lp.execute(null, null, null);
            }
        }
    }

    private void start() {
        questionTextview.setText(questions.get(id - 1));
        new CountDownTimer(5000, 1000) {
            public void onTick(long millisUntilFinished) {
                timerTextview.setText(String.valueOf(millisUntilFinished / 1000));
            }

            public void onFinish() {
                recordAnswer();
                if (id == questions.size()) {
                    showContentOrLoadingIndicator(true);
                }
            }
        }.start();
    }

    public Uri getOutputMediaFileUri(int id) {
        return Uri.fromFile(getOutputMediaFile(id));
    }

    private static File getOutputMediaFile(int id) {

        // External sdcard location
        File mediaStorageDir = new File(
                Environment.getExternalStorageDirectory(),
                "NaimiVideos");

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "can not make directory");
                return null;
            }
        }
        File mediaFile = new File(mediaStorageDir.getPath() + File.separator
                + "question" + id + ".mp4");
        return mediaFile;
    }

    /**
     * Cross-fades between {@link #mContentView} and {@link #mLoadingView}.
     */
    private void showContentOrLoadingIndicator(boolean contentLoaded) {
        // Decide which view to hide and which to show.
        final View showView = contentLoaded ? mContentView : mLoadingView;
        final View hideView = contentLoaded ? mLoadingView : mContentView;

        // Set the "show" view to 0% opacity but visible, so that it is visible
        // (but fully transparent) during the animation.
        showView.setAlpha(0f);
        showView.setVisibility(View.VISIBLE);

        // Animate the "show" view to 100% opacity, and clear any animation listener set on
        // the view. Remember that listeners are not limited to the specific animation
        // describes in the chained method calls. Listeners are set on the
        // ViewPropertyAnimator object for the view, which persists across several
        // animations.
        showView.animate()
                .alpha(1f)
                .setDuration(mShortAnimationDuration)
                .setListener(null);

        // Animate the "hide" view to 0% opacity. After the animation ends, set its visibility
        // to GONE as an optimization step (it won't participate in layout passes, etc.)
        hideView.animate()
                .alpha(0f)
                .setDuration(mShortAnimationDuration)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        hideView.setVisibility(View.GONE);
                    }
                });
    }

    private void recordAnswer() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        Uri fileUri = getOutputMediaFileUri(id++);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 120);
        //  intent.putExtra(VideoParameterSet.)
        // start the video capture Intent
        startActivityForResult(intent, 0);
        //startActivity(intent);

        /*

        */
    }


    private class LongOperation extends AsyncTask<String, Void, String> {
        private Context myContext;

        public LongOperation(Context myContext) {
            myContext = this.myContext;
        }

        @Override
        protected String doInBackground(String... params) {
            mergeVideo();
            return null;
        }


        @Override
        protected void onPostExecute(String result) {
            //   TextView txt = (TextView) findViewById(R.id.output);
            //  txt.setText("Executed");
            //  mContentLoaded = !mContentLoaded;
            endTime = System.nanoTime();
            long duration = (endTime - startTime)/1000000000;
            Log.d(TAG, "Duration" + duration/60 + "minutes " + duration+ "seconds");
            Log.d("Here is post execute", "Ooo");
            showContentOrLoadingIndicator(false);
            mLoadingView.setVisibility(View.GONE);
            mContentView.setVisibility(View.VISIBLE);
            // Show ready video

        }

        @Override
        protected void onPreExecute() {
            startTime = System.nanoTime();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

        private void mergeVideo() {
            root = Environment.getExternalStorageDirectory();
            //TextView tv = (TextView)findViewById(R.id.tv);
            //tv.setText(root.getAbsolutePath());
            ArrayList<String> videoUris = new ArrayList<>();
            ArrayList<Long> videoDurations = new ArrayList<>();
            File mainDir = new File(root, mainDirectory);
            if (!mainDir.exists()) {
                try {
                    if (!mainDir.mkdirs()) throw new Exception("Can not create directory");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            File outputFile = new File(mainDir, "output");
            if (!outputFile.exists()) {
                try {
                    if (!outputFile.mkdirs()) throw new Exception("Can not create directory");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for (File f : mainDir.listFiles()) {
                if (f.isFile()) {
                    String name = f.getName();
                    videoUris.add(root.getAbsolutePath() + mainDirectory + "/" + name);
                    retriever.setDataSource(myContext, Uri.fromFile(f));
                    String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long timeInMillisec = Long.parseLong(time);
                    Log.d(TAG, "File: " + f.getName() + " duration:" + timeInMillisec);
                    videoDurations.add(timeInMillisec);
                }
            }
            retriever.release();
            List<Movie> inMovies = new ArrayList<>();
            for (String videoUri : videoUris) {
                try {
                    inMovies.add(MovieCreator.build(videoUri));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            Log.d(TAG, "Size: " + inMovies.size());
            if (inMovies.size() != 0) {
                List<Track> videoTracks = new LinkedList<>();
                List<Track> audioTracks = new LinkedList<>();
                for (Movie m : inMovies) {
                    for (Track t : m.getTracks()) {
                        if (t.getHandler().equals("soun")) {
                            audioTracks.add(t);
                        }
                        if (t.getHandler().equals("vide")) {
                            videoTracks.add(t);
                        }
                    }
                }
                Movie result = new Movie();

                try {
                    if (!audioTracks.isEmpty()) {
                        result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
                    }
                    if (!videoTracks.isEmpty()) {
                        result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                TextTrackImpl textTrack = new TextTrackImpl();
                long durationCounter = 0;
                Log.d(TAG, "Questions size" + questions.size());
                Log.d(TAG, "Video duration size" + videoDurations.size());
                for (int i = 0; i < questions.size(); i++) {
                    long start = durationCounter;
                    long end = durationCounter + videoDurations.get(i);
                    textTrack.getSubs().add(new TextTrackImpl.Line(start, end, questions.get(i)));
                    durationCounter = end;
                }
                result.addTrack(textTrack);
                Container out = new DefaultMp4Builder().build(result);
                try {
                    FileChannel fc = new RandomAccessFile(root.getAbsolutePath() + mainDirectory + "/output/output.mp4", "rw").getChannel();
                    out.writeContainer(fc);
                    fc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}