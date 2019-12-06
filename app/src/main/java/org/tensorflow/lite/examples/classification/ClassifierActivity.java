
package org.tensorflow.lite.examples.classification;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import androidx.appcompat.app.AlertDialog;
import java.io.IOException;
import java.util.List;
import org.tensorflow.lite.examples.classification.env.BorderedText;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.Classifier;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;

public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final float TEXT_SIZE_DIP = 10;
  private Bitmap rgbFrameBitmap = null;
  private long lastProcessingTimeMs;
  private Integer sensorOrientation;
  private Classifier classifier;
  private BorderedText borderedText;
  /** Input image size of the model along x axis. */
  private int imageSizeX;
  /** Input image size of the model along y axis. */
  private int imageSizeY;


  @Override
  protected int getLayoutId() {
    return R.layout.camera_connection_fragment;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    recreateClassifier(getModel(), getDevice(), getNumThreads());
    if (classifier == null) {
      LOGGER.e("No classifier on preview!");
      return;
    }

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
  }

  @Override
  protected void processImage() {
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
    final int cropSize = Math.min(previewWidth, previewHeight);

    ImageButton eat = findViewById(R.id.eatitbutton); // declares the button
    eat.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v){
        // Eat It button is pressed
        classifier = null;  // stops the classifier from running in the background
        String meal = recognitionTextView.getText().toString(); // first classification

        // dialog object is created
        AlertDialog.Builder builder1 = new AlertDialog.Builder(ClassifierActivity.this);
        // question posed to user
        builder1.setMessage("Is the label '" + meal + "' correct?");
        builder1.setCancelable(false);

        // On click of the Yes button
        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                    // closes the dialog box
                    dialog.cancel();
                    // send label
                    setResult(ClassifierActivity.RESULT_OK, new Intent().putExtra("food", meal));
                    finish();
                  }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                  // when the No button is pressed, edittext asks for correct label
                  public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(ClassifierActivity.this);
                    alertDialog.setTitle("Label");
                    alertDialog.setMessage("Describe the correct label in 2 words or fewer: ");

                    final EditText input = new EditText(ClassifierActivity.this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
                    input.setLayoutParams(lp);
                    alertDialog.setView(input);

                    // User will type correct
                    alertDialog.setPositiveButton("Add",
                            new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                // send the new food with the calories
                                setResult(ClassifierActivity.RESULT_OK, new Intent().putExtra("food", input.getText().toString()));
                                finish();
                              }
                            });

                    alertDialog.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                              public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                              }
                            });

                    alertDialog.show();
                  }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
      }
    });

    // overrides the private handler in implemented activity
    final Handler handler = new Handler();

    handler.postDelayed(
        new Runnable() {
          @Override
          public void run() {
            if (classifier != null) {

                try {
                  final long startTime = SystemClock.uptimeMillis();
                  final List<Classifier.Recognition> results =
                          classifier.recognizeImage(rgbFrameBitmap, sensorOrientation);
                  lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;
                  LOGGER.v("Detect: %s", results);

                  runOnUiThread(
                          new Runnable() {
                            @Override
                            public void run() {
                              showResultsInBottomSheet(results);
                              showFrameInfo(previewWidth + "x" + previewHeight);
                              showCropInfo(imageSizeX + "x" + imageSizeY);
                              showCameraResolution(cropSize + "x" + cropSize);
                              showRotationInfo(String.valueOf(sensorOrientation));
                              showInference(lastProcessingTimeMs + "ms");
                            }
                          });
                }
                catch (NullPointerException e)
                {
                  e.printStackTrace();
                }
            }
            readyForNextImage();
          }
        }, 500);
    // adds a delay to the system of 1.5 seconds
  }

  @Override
  protected void onInferenceConfigurationChanged() {
    if (rgbFrameBitmap == null) {
      // Defer creation until we're getting camera frames.
      return;
    }
    final Device device = getDevice();
    final Model model = getModel();
    final int numThreads = getNumThreads();
    runInBackground(() -> recreateClassifier(model, device, numThreads));
  }

  private void recreateClassifier(Model model, Device device, int numThreads) {
    if (classifier != null) {
      LOGGER.d("Closing classifier.");
      classifier.close();
      classifier = null;
    }
    try {
      LOGGER.d(
          "Creating classifier (model=%s, device=%s, numThreads=%d)", model, device, numThreads);
      classifier = Classifier.create(this, model, device, numThreads);
    } catch (IOException e) {
      LOGGER.e(e, "Failed to create classifier.");
    }

    // Updates the input image size.
    imageSizeX = classifier.getImageSizeX();
    imageSizeY = classifier.getImageSizeY();
  }
}
