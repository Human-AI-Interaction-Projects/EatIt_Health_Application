package org.tensorflow.lite.examples.classification;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.lang.reflect.Array;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import org.tensorflow.lite.examples.classification.env.ImageUtils;
import org.tensorflow.lite.examples.classification.env.Logger;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Device;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Model;
import org.tensorflow.lite.examples.classification.tflite.Classifier.Recognition;
import org.w3c.dom.Text;

import static android.view.View.VISIBLE;

public abstract class CameraActivity extends AppCompatActivity
    implements OnImageAvailableListener,
        Camera.PreviewCallback,
        View.OnClickListener,
        AdapterView.OnItemSelectedListener {
  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;

  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;
  private LinearLayout bottomSheetLayout;
  private LinearLayout gestureLayout;
  private BottomSheetBehavior sheetBehavior;
  protected TextView recognitionTextView,
      recognition1TextView,
      recognition2TextView,
      recognitionValueTextView,
      recognition1ValueTextView,
      recognition2ValueTextView;
  protected TextView frameValueTextView,
      cropValueTextView,
      cameraResolutionTextView,
      rotationTextView,
      inferenceTimeTextView;
  protected ImageView bottomSheetArrowImageView;
  private ImageView plusImageView, minusImageView;
  private Spinner modelSpinner;
  private Spinner deviceSpinner;
  private TextView threadsTextView;

  private Model model = Model.FLOAT;
  private Device device = Device.CPU;
  private int numThreads = -1;

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.activity_camera);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayShowTitleEnabled(false);

    if (hasPermission()) {
      setFragment();
    } else {
      requestPermission();
    }

    threadsTextView = findViewById(R.id.threads);
    plusImageView = findViewById(R.id.plus);
    minusImageView = findViewById(R.id.minus);
    modelSpinner = findViewById(R.id.model_spinner);

    // Sets default to FLOAT
    modelSpinner.setSelection(1);

    deviceSpinner = findViewById(R.id.device_spinner);
    bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
    gestureLayout = findViewById(R.id.gesture_layout);
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
    bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);

    ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
              gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
              gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            //                int width = bottomSheetLayout.getMeasuredWidth();
            int height = gestureLayout.getMeasuredHeight();

            sheetBehavior.setPeekHeight(height);
          }
        });
    sheetBehavior.setHideable(false);

    sheetBehavior.setBottomSheetCallback(
        new BottomSheetBehavior.BottomSheetCallback() {
          @Override
          public void onStateChanged(@NonNull View bottomSheet, int newState) {
            switch (newState) {
              case BottomSheetBehavior.STATE_HIDDEN:
                break;
              case BottomSheetBehavior.STATE_EXPANDED:
                {
                  bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_down);
                }
                break;
              case BottomSheetBehavior.STATE_COLLAPSED:
                {
                  bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                }
                break;
              case BottomSheetBehavior.STATE_DRAGGING:
                break;
              case BottomSheetBehavior.STATE_SETTLING:
                bottomSheetArrowImageView.setImageResource(R.drawable.icn_chevron_up);
                break;
            }
          }

          @Override
          public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

    recognitionTextView = findViewById(R.id.detected_item);
    recognitionValueTextView = findViewById(R.id.detected_item_value);
    recognition1TextView = findViewById(R.id.detected_item1);
    recognition1ValueTextView = findViewById(R.id.detected_item1_value);
    recognition2TextView = findViewById(R.id.detected_item2);
    recognition2ValueTextView = findViewById(R.id.detected_item2_value);

    frameValueTextView = findViewById(R.id.frame_info);
    cropValueTextView = findViewById(R.id.crop_info);
    cameraResolutionTextView = findViewById(R.id.view_info);
    rotationTextView = findViewById(R.id.rotation_info);
    inferenceTimeTextView = findViewById(R.id.inference_info);

    // modelSpinner.setOnItemSelectedListener(this);
    // deviceSpinner.setOnItemSelectedListener(this);

    plusImageView.setOnClickListener(this);
    minusImageView.setOnClickListener(this);

    // model = Model.valueOf(modelSpinner.getSelectedItem().toString().toUpperCase());
    // device = Device.valueOf(deviceSpinner.getSelectedItem().toString());
    numThreads = Integer.parseInt(threadsTextView.getText().toString().trim());
  }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
        // onPreviewSizeChosen(new Size(100, 100), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
        new Runnable() {
          @Override
          public void run() {
            ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
          }
        };

    postInferenceCallback =
        new Runnable() {
          @Override
          public void run() {
            camera.addCallbackBuffer(bytes);
            isProcessingFrame = false;
          }
        };
    processImage();
  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
          new Runnable() {
            @Override
            public void run() {
              ImageUtils.convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);
            }
          };

      postInferenceCallback =
          new Runnable() {
            @Override
            public void run() {
              image.close();
              isProcessingFrame = false;
            }
          };

      processImage();
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    if (requestCode == PERMISSIONS_REQUEST) {
      if (grantResults.length > 0
          && grantResults[0] == PackageManager.PERMISSION_GRANTED
          && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
        setFragment();
      } else {
        requestPermission();
      }
    }
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;
    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
            .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
      CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
            (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                || isHardwareLevelSupported(
                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {
      CameraConnectionFragment camera2Fragment =
          CameraConnectionFragment.newInstance(
              new CameraConnectionFragment.ConnectionCallback() {
                @Override
                public void onPreviewSizeChosen(final Size size, final int rotation) {
                  previewHeight = size.getHeight();
                  previewWidth = size.getWidth();
                  CameraActivity.this.onPreviewSizeChosen(size, rotation);
                }
              },
              this,
              getLayoutId(),
              getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
          new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

  @UiThread
  protected void showResultsInBottomSheet(List<Recognition> results) {
    if (results != null && results.size() >= 3) {
      Recognition recognition = results.get(0);
      if (recognition != null) {
        if (recognition.getTitle() != null) {

          String food = recognition.getTitle();
          recognitionTextView.setText(food);

          // set the first 3 ingredents
          // String food = recognition.getTitle(); // string of first classification
          TextView ingredient1 = findViewById(R.id.frame);
          TextView calories1 = findViewById(R.id.frame_info);
          TextView ingredient2 = findViewById(R.id.crop);
          TextView calories2 = findViewById(R.id.crop_info);
          TextView ingredients3 = findViewById(R.id.view);
          TextView calories3 = findViewById(R.id.view_info);

          String ing1 = "";
          String ing2 = "";
          String ing3 = "";

          if (food.contains("Pizza")) {

            ing1 = "Flour";
            ingredient1.setText(ing1);
            calories1.setText("Grains");

            ing2 = "Mozarella Cheese";
            ingredient2.setText(ing2);
            calories2.setText("Dairy");

            ing3 = "Pepperoni";
            ingredients3.setText(ing3);
            calories3.setText("Protein");
          }

          if (food.contains("Tacos")) {

            ing1 = "Tortillas";
            ingredient1.setText(ing1);
            calories1.setText("Grains");

            ing2 = "Cheddar Cheese";
            ingredient2.setText(ing2);
            calories2.setText("Dairy");

            ing3 = "Ground Beef";
            ingredients3.setText(ing3);
            calories3.setText("Protein");
          }

          if (food.contains("Pancakes")) {

            ing1 = "Flour";
            ingredient1.setText(ing1);
            calories1.setText("Grains");

            ing2 = "Eggs";
            ingredient2.setText(ing2);
            calories2.setText("Protein");

            ing3 = "Butter";
            ingredients3.setText(ing3);
            calories3.setText("Dairy");
          }

          if (food.contains("Omelette")) {

            ing1 = "Egg";
            ingredient1.setText(ing1);
            calories1.setText("Protein");

            ing2 = "Cheddar Cheese";
            ingredient2.setText(ing2);
            calories2.setText("Dairy");

            ing3 = "Ham";
            ingredients3.setText(ing3);
            calories3.setText("Protein");
          }

          if (food.contains("Nachos")) {

            ing1 = "Tortilla Chips";
            ingredient1.setText(ing1);
            calories1.setText("Grains");

            ing2 = "Cheddar Cheese";
            ingredient2.setText(ing2);
            calories2.setText("Dairy");

            ing3 = "Peppers";
            ingredients3.setText(ing3);
            calories3.setText("Vegetables");
          }

          if (food.contains("Macaroni and Cheese")) {

            ing1 = "Flour";
            ingredient1.setText(ing1);
            calories1.setText("Grains");

            ing2 = "Cheddar Cheese";
            ingredient2.setText(ing2);
            calories2.setText("Dairy");

            ing3 = "Butter";
            ingredients3.setText(ing3);
            calories3.setText("Dairy");
          }

          if (food.contains("Lasagna")) {

            ing1 = "Flour";
            ingredient1.setText(ing1);
            calories1.setText("Grains");

            ing2 = "Beef";
            ingredient2.setText(ing2);
            calories2.setText("Protein");

            ing3 = "Mozarella Cheese";
            ingredients3.setText(ing3);
            calories3.setText("Dairy");
          }

          if (food.contains("Ice Cream")) {

            ing1 = "Sugar";
            ingredient1.setText(ing1);
            calories1.setText("Salts/Sugars");

            ing2 = "Milk";
            ingredient2.setText(ing2);
            calories2.setText("Dairy");

            ing3 = "Gelatin";
            ingredients3.setText(ing3);
            calories3.setText("Salts/Sugars");
          }

          if (food.contains("Hamburger")) {

            ing1 = "Beef";
            ingredient1.setText(ing1);
            calories1.setText("Protein");

            ing2 = "American Cheese";
            ingredient2.setText(ing2);
            calories2.setText("Dairy");

            ing3 = "Bread";
            ingredients3.setText(ing3);
            calories3.setText("Grains");
          }

          if (food.contains("Grilled Cheese")) {

            ing1 = "Bread";
            ingredient1.setText(ing1);
            calories1.setText("Grains");

            ing2 = "Butter";
            ingredient2.setText(ing2);
            calories2.setText("Dairy");

            ing3 = "Cheddar Cheese";
            ingredients3.setText(ing3);
            calories3.setText("Dairy");
          }

          if (food.contains("Frozen Yogurt")) {

            ing1 = "Milk";
            ingredient1.setText(ing1);
            calories1.setText("Dairy");

            ing2 = "Sugar";
            ingredient2.setText(ing2);
            calories2.setText("Salts/Sugars");

            ing3 = "Honey";
            ingredients3.setText(ing3);
            calories3.setText("Salts/Sugars");
          }

          if (food.contains("French Fries")) {

            ing1 = "Potatoes";
            ingredient1.setText(ing1);
            calories1.setText("Vegetables");

            ing2 = "Salt";
            ingredient2.setText(ing2);
            calories2.setText("Salts/Sugars");

            ing3 = "Oil";
            ingredients3.setText(ing3);
            calories3.setText("None");
          }

          if (food.contains("Chocolate Cake")) {

            ing1 = "Flour";
            ingredient1.setText(ing1);
            calories1.setText("Grains");

            ing2 = "Milk";
            ingredient2.setText(ing2);
            calories2.setText("Dairy");

            ing3 = "Egg";
            ingredients3.setText(ing3);
            calories3.setText("Protein");
          }

          // Now check for food restrictions

          // Lactose Intolerant
          ArrayList<String> diet0 = new ArrayList<String>();
          diet0.add("Cream");
          diet0.add("Milk");
          diet0.add("Cheese");

          // Vegetarian
          ArrayList<String> diet1 = new ArrayList<String>();
          diet1.add("Beef");

          ArrayList<String> diet2 = new ArrayList<String>();
          diet2.add("Pepperoni");
          diet2.add("Pork");
          diet2.add("Ham");

          ArrayList<String> diet3 = new ArrayList<String>();
          diet3.add("Cashews");
          diet3.add("Peanuts");
          diet3.add("Nuts");
          diet3.add("Almonds");
          diet3.add("Walnuts");

          ArrayList<String> diet4 = new ArrayList<String>();
          diet4.add("Bread");
          diet4.add("Gluten");
          diet4.add("Flour");
          diet4.add("Tortilla");

          ArrayList<String> diet5 = new ArrayList<String>();
          diet5.add("Fish");
          diet5.add("Shrimp");

          ArrayList<String> diet6 = new ArrayList<String>();
          diet6.add("Sugar");
          diet6.add("Honey");

          ArrayList<String> diet7 = new ArrayList<String>();
          diet7.add("Egg");
          // milk, beef, pork, nuts, gluten, seafood, sugar, egg
          // 0,       1,    2,    3,      4,       5,    6,   7

          ArrayList<ArrayList<String>> listOLists = new ArrayList<ArrayList<String>>();
          listOLists.add(diet0);
          listOLists.add(diet1);
          listOLists.add(diet2);
          listOLists.add(diet3);
          listOLists.add(diet4);
          listOLists.add(diet5);
          listOLists.add(diet6);
          listOLists.add(diet7);

          // grab the array from the bundle
          Bundle myBundle = getIntent().getExtras();
          // grab array from previous activity
          int[] diet= myBundle.getIntArray("diet");
          boolean badFood = false;  // there is no bad food

          for (int i = 0; i<diet.length; i++)
          {
            String lname = "diet";
            // each list of keywords correspond to the index in the array
            if (diet[i] == 1)
            {
              // create a
              for (int k = 0; k < listOLists.get(i).size(); k++)
              {
                if (ing1.contains(listOLists.get(i).get(k)) || ing2.contains(listOLists.get(i).get(k)) || ing3.contains(listOLists.get(i).get(k))) {
                  // that means the food contains a bad ingredient based upon the user's preference
                  badFood = true; // there is a bad food
                }
              }
            }
          }

          // sets the warning to not eat

          ImageView xmark = findViewById(R.id.xmark);
          ImageButton eatit = findViewById(R.id.eatitbutton);

          if (badFood)
          {
            // set the xMark on the screen
            xmark.setVisibility(View.VISIBLE);
            eatit.setVisibility(View.GONE);
            xmark.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT));
          }
          else
          {
            xmark.setVisibility(View.GONE);
            eatit.setVisibility(View.VISIBLE);
          }

          // need to loop through the array


        }
        if (recognition.getConfidence() != null)
          recognitionValueTextView.setText(
              String.format("%.2f", (100 * recognition.getConfidence())) + "%");
      }

      Recognition recognition1 = results.get(1);
      if (recognition1 != null) {
        if (recognition1.getTitle() != null) recognition1TextView.setText(recognition1.getTitle());
        if (recognition1.getConfidence() != null)
          recognition1ValueTextView.setText(
              String.format("%.2f", (100 * recognition1.getConfidence())) + "%");
      }

      Recognition recognition2 = results.get(2);
      if (recognition2 != null) {
        if (recognition2.getTitle() != null) recognition2TextView.setText(recognition2.getTitle());
        if (recognition2.getConfidence() != null)
          recognition2ValueTextView.setText(
              String.format("%.2f", (100 * recognition2.getConfidence())) + "%");
      }
    }
  }

  protected void showFrameInfo(String frameInfo) {
    // frameValueTextView.setText(frameInfo);
  }

  protected void showCropInfo(String cropInfo) {
    // cropValueTextView.setText(cropInfo);
  }

  protected void showCameraResolution(String cameraInfo) {
    // cameraResolutionTextView.setText(cameraInfo);
  }

  protected void showRotationInfo(String rotation) {
    // rotationTextView.setText(rotation);
  }

  protected void showInference(String inferenceTime) {
    inferenceTimeTextView.setText(inferenceTime);
  }

  protected Model getModel() {
    return model;
  }

  private void setModel(Model model) {
    if (this.model != model) {
      LOGGER.d("Updating  model: " + model);
      this.model = model;
      onInferenceConfigurationChanged();
    }
  }

  protected Device getDevice() {
    return device;
  }

  private void setDevice(Device device) {
    if (this.device != device) {
      LOGGER.d("Updating  device: " + device);
      this.device = device;
      final boolean threadsEnabled = device == Device.CPU;
      plusImageView.setEnabled(threadsEnabled);
      minusImageView.setEnabled(threadsEnabled);
      threadsTextView.setText(threadsEnabled ? String.valueOf(numThreads) : "N/A");
      onInferenceConfigurationChanged();
    }
  }

  protected int getNumThreads() {
    return numThreads;
  }

  private void setNumThreads(int numThreads) {
    if (this.numThreads != numThreads) {
      LOGGER.d("Updating  numThreads: " + numThreads);
      this.numThreads = numThreads;
      onInferenceConfigurationChanged();
    }
  }

  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void onInferenceConfigurationChanged();

  @Override
  public void onClick(View v) {
    if (v.getId() == R.id.plus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads >= 9) return;
      setNumThreads(++numThreads);
      threadsTextView.setText(String.valueOf(numThreads));
    } else if (v.getId() == R.id.minus) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads == 1) {
        return;
      }
      setNumThreads(--numThreads);
      threadsTextView.setText(String.valueOf(numThreads));
    }
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
  }
}
