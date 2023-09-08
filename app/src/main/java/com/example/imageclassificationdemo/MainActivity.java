package com.example.imageclassificationdemo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    protected Interpreter tflite;
    private MappedByteBuffer tfliteModel;
    private TensorImage inputImageBuffer;
    private  int imageSizeX;
    private  int imageSizeY;
    private  TensorBuffer outputProbabilityBuffer;
    private  TensorProcessor probabilityProcessor;
    private static final float IMAGE_MEAN = 0.0f;
    private static final float IMAGE_STD = 1.0f;
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 255.0f;
    private Bitmap bitmap;
    private List<String> labels;

    Uri imageuri;
    ImageView imageView;
    Button buttonclassify,btnTreatment;
    TextView classitext;
    int i=0; // to know if user select a image or not (if i=0 this meaning the user didn't select image )

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView=(ImageView)findViewById(R.id.image);
        buttonclassify=(Button)findViewById(R.id.classify);
        classitext=(TextView)findViewById(R.id.classifytext);
        btnTreatment=findViewById(R.id.treatment);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (i==0) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), 12);
                }
                else {
                     AlertDialog.Builder mAlertDialog;
                    mAlertDialog=new AlertDialog.Builder(MainActivity.this);
                    mAlertDialog.setTitle("ATTENTION");
                    mAlertDialog.setMessage("Do you want a new diagnosis ?");
                    mAlertDialog.setCancelable(false);
                    mAlertDialog.setPositiveButton("No",
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();

                        }
                    });
                    mAlertDialog.setNegativeButton("YES", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            recreate();
                        }
                    });

                    AlertDialog dialog= mAlertDialog.create();
                    dialog.show();
                }
            }// end onClick (imageView)
        });

        try{

            tflite=new Interpreter(loadmodelfile(this));
            //  loadmodelfile method is to laod 1st machine learning model.

           /*
           This 1st machine learning model is to Ensure that
            the user has entered a picture of the face of a person with a skin disease .
            */

        }catch (Exception e) {
            e.printStackTrace();
        }



        buttonclassify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(i==0){
                    // if user click on (buttonclassify button) and he didn't select an image
                    Toast.makeText(MainActivity.this,
                            "Please select an Image ",Toast.LENGTH_SHORT).show();

                }else {
                // if user click on (buttonclassify button) and he selected an image

                    int imageTensorIndex = 0;
                    int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
                    imageSizeY = imageShape[1];
                    imageSizeX = imageShape[2];
                    DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

                    int probabilityTensorIndex = 0;
                    int[] probabilityShape =
                            tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
                    DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

                    inputImageBuffer = new TensorImage(imageDataType);
                    outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
                    probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

                    inputImageBuffer = loadImage(bitmap);

                    tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());
                    showresult();
                }



            }// end onClick (buttonclassify)
        });

        btnTreatment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
/*
               this button treatment (that takes it to another activity
               to show the suggested treatment ) if the user has entered a valid image
               hint: if the user entered a Invalid image this button is not visible
 */

                String disease=classitext.getText().toString();

                Intent mIntent =new Intent(getApplicationContext(),Treatments.class);
                //purpose of treatment activity is to show suggested treatment

                mIntent.putExtra("the disease",disease);
                startActivity(mIntent);
            }// end onClick
        });




    }// end onCreate

    private TensorImage loadImage(final Bitmap bitmap) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        // TODO(b/143564309): Fuse ops inside ImageProcessor.
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
                        .add(getPreprocessNormalizeOp())
                        .build();

        return imageProcessor.process(inputImageBuffer);
    }//end loadImage

    private MappedByteBuffer loadmodelfile(Activity activity) throws IOException {
        //  loadmodelfile method is to laod 1st machine learning model (model's name is:newmodell.tflite).

        AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("newmodell.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    }//end loadmodelfile method

    private MappedByteBuffer loadmodelfile2(Activity activity) throws IOException {
        //  loadmodelfile method is to laod 2nd machine learning model (model's name is:newmodel.tflite).
        AssetFileDescriptor fileDescriptor=activity.getAssets().openFd("newmodel.tflite");
        FileInputStream inputStream=new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel=inputStream.getChannel();
        long startoffset = fileDescriptor.getStartOffset();
        long declaredLength=fileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY,startoffset,declaredLength);
    } //end loadmodelfile2 method


    private TensorOperator getPreprocessNormalizeOp() {

        return new NormalizeOp(IMAGE_MEAN, IMAGE_STD);
    }//end getPreprocessNormalizeOp
    private TensorOperator getPostprocessNormalizeOp(){

        return new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD);
    } //end getPostprocessNormalizeOp

    private void showresult(){
        /*
         Those coming lines of code for prepare the 1st machine learning model.

         This 1st machine learning model is to Ensure that
         the user has entered a picture of the face of a person with a skin disease
         */

        try{
            labels = FileUtil.loadLabels(this,"newdictl.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Float> labeledProbability =
                new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                        .getMapWithFloatValue();
        float maxValueInMap =(Collections.max(labeledProbability.values()));

        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue()==maxValueInMap) {
                    if(entry.getKey().equals("face")){
                        // entry.getKey() is show o/p of 1st machine learning model
    // to Ensure that the user has entered a picture of the face of a person with a skin disease

                        showresult2();
                        /* the purpose of this method:
                         if the user has entered a picture of the face of a person with a skin disease,
                         Go to (showresult2 method) to go 2nd machine learning model
                         to diagnose the skin disease.
                         */

                        btnTreatment.setVisibility(View.VISIBLE);

                        /*
                         The button treatment (that takes it to another activity
                          to show the suggested treatment ) will be visible if the user has entered a valid image

                         */

                    }else {
                        Toast.makeText(this, "Please, Enter a correct image  ", Toast.LENGTH_SHORT).show();

                    }

            }
        }
    }// end showresult method

    private void showresult2(){
        try{
            tflite=new Interpreter(loadmodelfile2(this));
        }catch (Exception e) {
            e.printStackTrace();
        }


        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();

        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        inputImageBuffer = new TensorImage(imageDataType);
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);
        probabilityProcessor = new TensorProcessor.Builder().add(getPostprocessNormalizeOp()).build();

        inputImageBuffer = loadImage(bitmap);

        tflite.run(inputImageBuffer.getBuffer(),outputProbabilityBuffer.getBuffer().rewind());

        try{
            labels = FileUtil.loadLabels(this,"newdict.txt");
        }catch (Exception e){
            e.printStackTrace();
        }
        Map<String, Float> labeledProbability =
                new TensorLabel(labels, probabilityProcessor.process(outputProbabilityBuffer))
                        .getMapWithFloatValue();
        float maxValueInMap =(Collections.max(labeledProbability.values()));

        for (Map.Entry<String, Float> entry : labeledProbability.entrySet()) {
            if (entry.getValue()==maxValueInMap) {
                classitext.setText(entry.getKey());
               // entry.getKey() is show o/p of 2nd machine learning model
            }
        }
    }// end showresult2 method

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode==12 && resultCode==RESULT_OK && data!=null) {
            imageuri = data.getData();
            i=1;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageuri);
                imageView.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }// end onActivityResult
}// end mainActivity class

