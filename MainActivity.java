package com.example.nurdaulet.naimi;

import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //collecting questions
        questions.add("Ваше полное имя, должность и опыт работы");
        questions.add("Чем Вас привлекает работа у нас в данной должности?");
        questions.add("Каковы Ваши сильные стороны?");
        questions.add("Каковы Ваши слабые стороны?");
        questions.add("Почему Вы считаете себя достойным занять эту должность? В чем Ваши преимущества перед другими кандидатами?");
        questions.add("Получали ли Вы другие предложения работы?");

        final Button start = (Button) findViewById(R.id.start);
        timerTextview = (TextView) findViewById(R.id.timer);
        questionTextview = (TextView) findViewById(R.id.question);
        questionTextview.setText(questions.get(id - 1));
        retriever = new MediaMetadataRetriever();

        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //recordAnswer();
                LongOperation lp = new LongOperation(getApplicationContext());
                lp.execute(null, null, null);
                start.setVisibility(View.GONE);
            }
        });
/*
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mergeVideo();
                start.setVisibility(View.GONE);
            }
        });*/
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (id != questions.size() + 1) {
                questionTextview.setText(questions.get(id - 1));
                new CountDownTimer(5000, 1000) {
                    public void onTick(long millisUntilFinished) {
                        timerTextview.setText(String.valueOf(millisUntilFinished / 1000));
                    }

                    public void onFinish() {
                        recordAnswer();
                    }
                }.start();
                Toast.makeText(getApplicationContext(), "Video capture was completed", Toast.LENGTH_LONG).show();
            } else {
                LongOperation lp = new LongOperation(this);
                lp.execute(null, null, null);
            }
        }
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

    private void recordAnswer() {
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        Uri fileUri = getOutputMediaFileUri(id++);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 3);
      //  intent.putExtra(VideoParameterSet.)
        // start the video capture Intent
        startActivityForResult(intent, 27);
        /*

        */
    }


    private class LongOperation extends AsyncTask<String, Void, String> {
        private Context myContext;

        public LongOperation(Context myContext){
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

        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }

        private void mergeVideo() {

            File root = Environment.getExternalStorageDirectory();
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

            for (File f : mainDir.listFiles()) {
                if (f.isFile()) {
                    String name = f.getName();
                    videoUris.add(root.getAbsolutePath() + mainDirectory + "/" + name);
//use one of overloaded setDataSource() functions to set your data source
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