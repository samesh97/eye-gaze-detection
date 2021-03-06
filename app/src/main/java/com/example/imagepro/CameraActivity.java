package com.example.imagepro;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;


import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Random;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{
    private static final String TAG="MainActivity";


    private Button button;
    private Mat mRgba;
    private CameraBridgeViewBase mOpenCvCameraView;
    private FacialLandmarkDetection facialDetection;
    private GestureDetection gestureDetection;

    private ArrayList<View> viewsInDisplay = new ArrayList<>();

    private Bitmap image = null;



    private BaseLoaderCallback mLoaderCallback =new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status){
                case LoaderCallbackInterface
                        .SUCCESS:{
                    Log.i(TAG,"OpenCv Is loaded");
                    mOpenCvCameraView.enableView();
                }
                default:
                {
                    super.onManagerConnected(status);

                }
                break;
            }
        }
    };

    public CameraActivity(){
        Log.i(TAG,"Instantiated new "+this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        int MY_PERMISSIONS_REQUEST_CAMERA=0;
        // if camera permission is not given it will ask for it on device
        if (ContextCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED){
            ActivityCompat.requestPermissions(CameraActivity.this, new String[] {Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA);
        }

        setContentView(R.layout.activity_camera);

        mOpenCvCameraView=(CameraBridgeViewBase) findViewById(R.id.frame_Surface);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCameraIndex(1);
        mOpenCvCameraView.setCvCameraViewListener(this);


        button = findViewById(R.id.button);
        Button btn1 = findViewById(R.id.button1);
        Button btn2 = findViewById(R.id.button2);
        Button btn3 = findViewById(R.id.button3);
        viewsInDisplay.add(button);
        viewsInDisplay.add(btn1);
        viewsInDisplay.add(btn2);
        viewsInDisplay.add(btn3);




        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        int height = metrics.heightPixels;
        int width = metrics.widthPixels;

        try
        {
            int inputImageSize = 150;

            facialDetection = new FacialLandmarkDetection(getAssets(), CameraActivity.this, "facialLandmarkDetectionModel.tflite", inputImageSize,height,width ,new OnLandmarkResultChanged() {
                @Override
                public void onFaceDrawn(Mat mat)
                {
                    Bitmap bitmap = null;
                    bitmap = Bitmap.createBitmap(mat.cols(),mat.rows(),Bitmap.Config.ARGB_8888);
                    Utils.matToBitmap(mat,bitmap);

                    Bitmap finalBitmap = bitmap;
                    image = finalBitmap;

                    String gesture = gestureDetection.detectGestures(finalBitmap);

                    TextView tv = findViewById(R.id.txt1);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run()
                        {
                            tv.setText(gesture + " Gesture Detected");
                        }
                    });

                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ImageView b = findViewById(R.id.xxxz);
                            b.setImageBitmap(finalBitmap);
                        }
                    });
                }

                @Override
                public void onCoordinatesChanged(int x, int y) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {



                            ImageView b = findViewById(R.id.xxx);

                            b.animate().x(x);
                            b.animate().y(y);

                            findViewPoint(x,y);



                        }
                    });
                }
            });
        }
        catch (IOException e){ e.printStackTrace(); }

        try
        {
            gestureDetection = new GestureDetection(getAssets(),CameraActivity.this,"gestureDetectionModel.tflite",224);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }
    void findViewPoint(int x,int y)
    {
        for(View view : viewsInDisplay)
        {
            int[] location = new int[2];
            view.getLocationOnScreen(location);

            int viewX = location[0];
            int viewY = location[1];

            Log.d("aaaaaaaaaaaa","X - " + x + ", View X - " + (viewX - view.getWidth()));

            int viewMaxWidth = viewX + view.getWidth();
            int viewMinWidth = viewX - view.getWidth();

            int viewMaxHeight = viewY + view.getHeight();
            int viewMinHeight = viewY - view.getHeight();

            if((x >= viewMinWidth && x <= viewMaxWidth) && (y >= viewMinHeight && y <= viewMaxHeight))
            {
                view.setBackgroundColor(Color.RED);
                ImageView b = findViewById(R.id.xxx);

                b.animate().x((viewMinWidth + viewMaxWidth) / 2);
                b.animate().y((viewMinHeight + viewMaxHeight) / 2);
            }
            else
            {
                view.setBackgroundColor(Color.BLACK);
            }

        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (OpenCVLoader.initDebug())
        {
            //if load success
            Log.d(TAG,"Opencv initialization is done");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
        else
        {
            //if not loaded
            Log.d(TAG,"Opencv is not loaded. try again");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_4_0,this,mLoaderCallback);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView !=null)
        {
            mOpenCvCameraView.disableView();
        }
    }

    public void onDestroy()
    {
        super.onDestroy();
        if(mOpenCvCameraView !=null)
        {
            mOpenCvCameraView.disableView();
        }

    }

    public void onCameraViewStarted(int width ,int height)
    {
        mRgba=new Mat(height,width, CvType.CV_8UC4);
    }
    public void onCameraViewStopped(){
        mRgba.release();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {

        mRgba=inputFrame.rgba();

        Core.flip(mRgba,mRgba,-1);

        Mat original = new Mat();
        mRgba.copyTo(original);

        Mat out=new Mat();
        // pass real-time frame in recognizeImage


        out = facialDetection.recognizeImage(mRgba);
        // Display out Mat image
        return mRgba;

    }
    private void saveImage(Bitmap bitmap, @NonNull String folderName) throws IOException
    {
        boolean saved;
        OutputStream fos;

        Random r = new Random();
        int v = r.nextInt(10000000);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = getApplicationContext().getContentResolver();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, v);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/" + folderName);
            Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            fos = resolver.openOutputStream(imageUri);
        }
        else
        {
            String imagesDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM).toString() + File.separator + folderName;

            File file = new File(imagesDir);

            if (!file.exists()) {
                file.mkdir();
            }



            File image = new File(imagesDir, v + ".png");
            fos = new FileOutputStream(image);

        }

        saved = bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        fos.flush();
        fos.close();
    }

    public void SaveLeftImage(View view)
    {
        if(image != null) {
            try {
                saveImage(image,"Top");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void SaveRightImage(View view)
    {
        if(image != null) {
            try {
                saveImage(image,"Bottom");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}