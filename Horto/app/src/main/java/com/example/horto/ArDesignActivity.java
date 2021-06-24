/* New class containing methods and attributes from hello_ar_java project made by Google
   Class constructed from multiple classes:
        - CameraPermissionHelper
        - DepthSettings
        - DisplayRotationHelper
        - FullScreenHelper
        - TapHelper
        - HelloArActivity

   All modifications made over duration of the project.
   Link to sample project: https://github.com/google-ar/arcore-Android-sdk.git
   * The comment: 'My addition' has been made to the areas I have added *
 */


package com.example.horto;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Image;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.Display;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.horto.common.GardenDesign;
import com.example.horto.common.PlantPosition;
import com.example.horto.common.SaveDesignDialog;
import com.example.horto.common.android_helpers.SnackbarHelper;
import com.example.horto.common.android_helpers.TrackingStateHelper;
import com.example.horto.common.opengl.Framebuffer;
import com.example.horto.common.opengl.Mesh;
import com.example.horto.common.arcore.PlantAnchor;
import com.example.horto.common.opengl.Render;
import com.example.horto.common.opengl.Shader;
import com.example.horto.common.opengl.VertexBuffer;
import com.example.horto.common.arcore.BackgroundRenderer;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.example.horto.common.arcore.PlaneRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;

public class ArDesignActivity extends AppCompatActivity implements Render.Renderer, View.OnTouchListener, SaveDesignDialog.SaveDialogListener {

    private static final String TAG = ArDesignActivity.class.getSimpleName();

    // Messages for Snackbar
    private static final String searchingPlaneMessage = "Looking for a surface...";
    private static final String waitingMessage = "Tap in the green area to place a plant...";

    // Camera Permission Check
    private static final int CAMERA_PERMISSION_CODE = 0;
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    // Distance of recognition
    private static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

    // glSurfaceView UI Component
    private GLSurfaceView glSurfaceView;

    // AR Session
    private Session session;

    // Snackbar Helper
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);

    private GestureDetector gestureDetector;
    private BlockingQueue<MotionEvent> queuedSingleTaps;

    // Rendering Objects
    private PlaneRenderer planeRenderer;
    private BackgroundRenderer backgroundRenderer;
    private Framebuffer virtualSceneFrameBuffer;
    private boolean hasSetTextureNames = false;

    private boolean installRequested;

    // Depth settings
    public static final String SHARED_PREFERENCES_ID_DEPTH = "SHARED_PREFERENCES_OCCLUSION_OPTIONS";
    public static final String SHARED_PREFERENCES_USE_DEPTH_FOR_OCCLUSION = "use_depth_for_occlusion";
    private boolean useDepthForOcclusion = false;
    private SharedPreferences sharedPreferencesDepth;

    // Point Cloud
    private VertexBuffer pointCloudVertexBuffer;
    private Mesh pointCloudMesh;
    private Shader pointCloudShader;

    // Keep track of the last point cloud rendered to avoid updating the VBO if point cloud
    // was not changed.  Do this using the timestamp since we can't compare PointCloud objects.
    private long lastPointCloudTimestamp = 0;

    // Virtual object (Plants)
    private Mesh virtualObjectMesh;
    private Shader virtualObjectShader;
    private final ArrayList<PlantAnchor> plantAnchors = new ArrayList<>();

    // Temporary matrix allocated here to reduce number of allocations for each frame.
    private final float[] modelMatrix = new float[16];
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] modelViewMatrix = new float[16]; // view x model
    private final float[] modelViewProjectionMatrix = new float[16]; // projection x view x model

    // Display Rotation
    private boolean viewportChanged;
    private int viewportWidth;
    private int viewportHeight;
    private Display display;

    // Model paths and Meshes
    private ArrayList<String> modelPaths = new ArrayList<>();
    private ArrayList<Mesh> meshes = new ArrayList<>();
    private String currentModel = "";

    // Garden design attributes
    private GardenDesign gardenDesign;
    private RealmList<PlantPosition> plantPositions;
    private String designTitle;

    // Toast properties
    CharSequence text = "Max plants reached";
    int duration = Toast.LENGTH_SHORT;
    Toast toast;
    Toast toastPlant;


    // On creation of activity
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ar_view);
        glSurfaceView = findViewById(R.id.view_ar);

        // Enable AR-related functionality on ARCore supported devices only.
        maybeEnableArButton();

        // Touch Listener
        queuedSingleTaps = new ArrayBlockingQueue<>(16);
        gestureDetector =
                new GestureDetector(
                        this,
                        new GestureDetector.SimpleOnGestureListener() {
                            @Override
                            public boolean onSingleTapUp(MotionEvent e) {
                                // Queue tap if there is space. Tap is lost if queue is full.
                                queuedSingleTaps.offer(e);
                                return true;
                            }

                            @Override
                            public boolean onDown(MotionEvent e) {
                                return true;
                            }
                        });

        // My addition
        // Add model paths to the modelPath list
        modelPaths.add("models/CoffeePlant.obj");
        modelPaths.add("models/Bromeliads.obj");
        modelPaths.add("models/dandelion.obj");
        modelPaths.add("models/FiddleleafFigPottedPlant.obj");


        // Set Touch Listener
        glSurfaceView.setOnTouchListener(this);

        // Renderer
        Render render = new Render(glSurfaceView, this, getAssets());
        display = this.getDisplay();

        installRequested = false;

        // Depth Settings InitialisE
        sharedPreferencesDepth = this.getSharedPreferences(SHARED_PREFERENCES_ID_DEPTH, Context.MODE_PRIVATE);
        useDepthForOcclusion =
                sharedPreferencesDepth.getBoolean(SHARED_PREFERENCES_USE_DEPTH_FOR_OCCLUSION, false);

        // My addition
        // Back button to go to home screen
        Button backButton = findViewById(R.id.back_ar_button);
        backButton.setOnClickListener(v -> {
            Intent myIntent = new Intent(v.getContext(), HomePageActivity.class);
            startActivityForResult(myIntent, 0);
        });
        // My addition
        // Bottom Panel for model buttons
        View bottomSheet = findViewById(R.id.bottomSheet);
        BottomSheetBehavior<View> mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        // My addition
        // Coffee plant button
        Button coffeeButton = findViewById(R.id.modelButton1);
        coffeeButton.setOnClickListener(v -> {
                currentModel = "coffeePlant";
                toastPlant = Toast.makeText(this, "Coffee Plant Chosen", duration);
                toastPlant.show();
        });
        // My addition
        // Bromeliad plant button
        Button bromeliadButton = findViewById(R.id.modelButton2);
        bromeliadButton.setOnClickListener(v -> {
            currentModel = "bromeliad";
            toastPlant = Toast.makeText(this, "Bromeliad Chosen", duration);
            toastPlant.show();
        });
        // My addition
        // Dandelion plant button
        Button dandelionButton = findViewById(R.id.modelButton3);
        dandelionButton.setOnClickListener(v -> {
            currentModel = "dandelion";
            toastPlant = Toast.makeText(this, "Dandelion Chosen", duration);
            toastPlant.show();
        });
        // My addition
        // Fiddle plant button
        Button fiddleButton = findViewById(R.id.modelButton4);
        fiddleButton.setOnClickListener(v -> {
            currentModel = "fiddleFig";
            toastPlant = Toast.makeText(this, "FiddleFig Chosen", duration);
            toastPlant.show();
        });
        // My addition
        // Design save button
        Button saveButton = findViewById(R.id.save_design_button);
        saveButton.setOnClickListener(v ->{
        //  If no plants placed, cancel saving
        if (!plantAnchors.isEmpty()) openSaveDialog();
        else {
            toast = Toast.makeText(this, "Your design is empty...", duration);
            toast.show();
        }});
        // My addition
        // View Plants list button
        Button viewDesignButton = findViewById(R.id.view_designs_button);
        viewDesignButton.setOnClickListener(v -> {
            Intent myIntent = new Intent(v.getContext(), DesignListActivity.class);
            startActivityForResult(myIntent, 0);
            ArDesignActivity.this.finish();
        });
        // My addition
        // Garden design attributes initialised
        gardenDesign = new GardenDesign();
        plantPositions = new RealmList<>();
        toast = Toast.makeText(this, text, duration);

    }
    // My addition
    // Save dialog action
    public void openSaveDialog() {
        SaveDesignDialog svd = new SaveDesignDialog();
        svd.show(getSupportFragmentManager(), "Save Dialog");
    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }

    // Enable ARCore
    void maybeEnableArButton() {
        ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(this);
        if (availability.isTransient()) {
            // Continue to query availability at 5Hz while compatibility is checked in the background.
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    maybeEnableArButton();
                }
            }, 200);
        }
    }

    // Get Camera permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (!(ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED)) {
            // Use toast instead of snackbar here since the activity will exit.
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, CAMERA_PERMISSION)) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.fromParts("package", this.getPackageName(), null));
                this.startActivity(intent);
            }
            finish();
        }
    }

    // My addition
    //  On resume of the activity
    @Override
    protected void onResume() {
        super.onResume();
        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                switch (ArCoreApk.getInstance().requestInstall(this, !installRequested)) {
                    case INSTALL_REQUESTED:
                        installRequested = true;
                        return;
                    case INSTALLED:
                        break;
                }
                // Check for camera permission
                if (!(ContextCompat.checkSelfPermission(this, CAMERA_PERMISSION) == PackageManager.PERMISSION_GRANTED)) {
                    ActivityCompat.requestPermissions(
                            this, new String[]{CAMERA_PERMISSION}, CAMERA_PERMISSION_CODE);
                    Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                // Create a new AR Session
                session = new Session(this);
            } catch (UnavailableUserDeclinedInstallationException e) {
                message = "Please install ARCore";
            } catch (UnavailableDeviceNotCompatibleException e) {
                message = "This device does not support AR";
            } catch (Exception e) {
                message = "Failed to create AR session";
            }

            if (message != null) {
                messageSnackbarHelper.showError(this, message);
                Log.e(TAG, "Exception creating session", exception);
                return;
            }
        }

        // Resume session if session is not null
        try {
            configureSession();
            session.resume();
        } catch (CameraNotAvailableException e) {
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            session = null;
            return;
        }
        glSurfaceView.onResume();
    }

    // On pause of the activity
    @Override
    public void onPause() {
        super.onPause();
        if (session != null) {
            glSurfaceView.onPause();
            session.pause();
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            this
                    .getWindow()
                    .getDecorView();
        }
    }

    @Override
    public void onSurfaceCreated(Render render) {
        // Prepare the rendering objects.
        try {
            // Initialise Plane Renderer
            planeRenderer = new PlaneRenderer(render);
            backgroundRenderer = new BackgroundRenderer(render);
            virtualSceneFrameBuffer = new Framebuffer(render, 1, 1);

            // Point cloud
            pointCloudShader =
                    Shader.createFromAssets(
                            render, "shaders/point_cloud.vert", "shaders/point_cloud.frag", null)
                            .setVec4(
                                    "u_Color", new float[]{0.3f, 0.8f, 0.3f, 1.0f})
                            .setFloat("u_PointSize", 10.0f);

            // four entries per vertex: X, Y, Z, confidence
            pointCloudVertexBuffer =
                    new VertexBuffer(render, /*numberOfEntriesPerVertex=*/ 4, /*entries=*/ null);

            final VertexBuffer[] pointCloudVertexBuffers = {pointCloudVertexBuffer};

            pointCloudMesh =
                    new Mesh(
                            render, Mesh.PrimitiveMode.POINTS, /*indexBuffer=*/ null, pointCloudVertexBuffers);

            // Creates meshes from the model paths and store within list.
            // My addition
            for (String path : modelPaths) {
                Mesh mesh = Mesh.createFromAsset(render, path);
                meshes.add(mesh);
            }
            // My addition
            // Set the default mesh selected.
            virtualObjectMesh = meshes.get(0);
            currentModel = "coffeePlant";

            virtualObjectShader = Shader.createFromAssets(render,
                    "shaders/mesh_shader.vert",
                    "shaders/mesh_shader.frag", null);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSurfaceChanged(Render render, int width, int height) {
        viewportWidth = width;
        viewportHeight = height;
        viewportChanged = true;
        virtualSceneFrameBuffer.resize(width, height);
    }

    // Occur on each frame draw
    @Override
    public void onDrawFrame(Render render) {
        if (session == null) {
            return;
        }

        if (!hasSetTextureNames) {
            session.setCameraTextureNames(
                    new int[]{backgroundRenderer.getCameraColorTexture().getTextureId()});
            hasSetTextureNames = true;
        }

        // -- Update per-frame state

        // Notify ARCore session that the view size changed so that the perspective matrix and
        // the video background can be properly adjusted.
        if (viewportChanged) {
            int displayRotation = display.getRotation();
            session.setDisplayGeometry(displayRotation, viewportWidth, viewportHeight);
            viewportChanged = false;
        }

        // Obtain the current frame from ARSession. When the configuration is set to
        // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
        // camera framerate.
        Frame frame;
        try {
            frame = session.update();
        } catch (CameraNotAvailableException e) {
            Log.e(TAG, "Camera not available during onDrawFrame", e);
            messageSnackbarHelper.showError(this, "Camera not available. Try restarting the app.");
            return;
        }
        Camera camera = frame.getCamera();

        // Update BackgroundRenderer state to match the depth settings.
        try {
            backgroundRenderer.setUseDepthVisualization(
                    render, false);
            backgroundRenderer.setUseOcclusion(render, useDepthForOcclusion);
        } catch (IOException e) {
            Log.e(TAG, "Failed to read a required asset file", e);
            messageSnackbarHelper.showError(this, "Failed to read a required asset file: " + e);
            return;
        }
        // BackgroundRenderer.updateDisplayGeometry must be called every frame to update the coordinates
        // used to draw the background camera image.
        backgroundRenderer.updateDisplayGeometry(frame);

        if (camera.getTrackingState() == TrackingState.TRACKING
                && (useDepthForOcclusion)) {
            try (Image depthImage = frame.acquireDepthImage()) {
                backgroundRenderer.updateCameraDepthTexture(depthImage);
            } catch (NotYetAvailableException e) {
                // This normally means that depth data is not available yet. This is normal so we will not
                // spam the logcat with this.
            }
        }

        // Handle one tap per frame.
        handleTap(render, frame, camera);

        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        // Show a message based on whether tracking has failed, if planes are detected, and if the user
        // has placed any objects.
        String message = null;
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
                message = searchingPlaneMessage;
            } else {
                message = TrackingStateHelper.getTrackingFailureReasonString(camera);
            }
        } else if (hasTrackingPlane()) {
            if (plantAnchors.isEmpty()) {
                message = waitingMessage;
            }
        } else {
            message = searchingPlaneMessage;
        }
        if (message == null) {
            messageSnackbarHelper.hide(this);
        } else {
            messageSnackbarHelper.showMessage(this, message);
        }

        // -- Draw background
        if (frame.getTimestamp() != 0) {
            // Suppress rendering if the camera did not produce the first frame yet. This is to avoid
            // drawing possible leftover data from previous sessions if the texture is reused.
            backgroundRenderer.drawBackground(render);
        }

        // If not tracking, don't draw 3D objects.
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            return;
        }

        // -- Draw non-occluded virtual objects (planes, point cloud)

        // Get projection matrix.
        camera.getProjectionMatrix(projectionMatrix, 0, 0.1f, 100f);

        // Get camera matrix and draw.
        camera.getViewMatrix(viewMatrix, 0);

        // Visualize tracked points.
        // Use try-with-resources to automatically release the point cloud.
        try (PointCloud pointCloud = frame.acquirePointCloud()) {
            if (pointCloud.getTimestamp() > lastPointCloudTimestamp) {
                pointCloudVertexBuffer.set(pointCloud.getPoints());
                lastPointCloudTimestamp = pointCloud.getTimestamp();
            }
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
            pointCloudShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            render.draw(pointCloudMesh, pointCloudShader);
        }

        // Visualize planes.
        planeRenderer.drawPlanes(
                render,
                session.getAllTrackables(Plane.class),
                camera.getDisplayOrientedPose(),
                projectionMatrix);

        // Visualize anchors created by touch.
        render.clear(virtualSceneFrameBuffer, 0f, 0f, 0f, 0f);

        // My addition
        for (PlantAnchor anchor : plantAnchors) {
            if (anchor.getAnchor().getTrackingState() != TrackingState.TRACKING) {
                continue;
            }
            try {
                // Check anchor type to render correct model upon.
                String currentAnchor = anchor.getType();
                switch (currentAnchor) {
                    case "coffeePlant":
                        virtualObjectMesh = meshes.get(0);
                        break;
                    case "bromeliad":
                        virtualObjectMesh = meshes.get(1);
                        break;
                    case "dandelion":
                        virtualObjectMesh = meshes.get(2);
                        break;
                    case "fiddleFig":
                        virtualObjectMesh = meshes.get(3);
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // Get the current pose of an Anchor in world space. The Anchor pose is updated
            // during calls to session.update() as ARCore refines its estimate of the world.
            anchor.getAnchor().getPose().toMatrix(modelMatrix, 0);

            // Calculate model/view/projection matrices
            Matrix.multiplyMM(modelViewMatrix, 0, viewMatrix, 0, modelMatrix, 0);
            Matrix.multiplyMM(modelViewProjectionMatrix, 0, projectionMatrix, 0, modelViewMatrix, 0);

            // Update shader properties and draw
            virtualObjectShader.setMat4("u_ModelView", modelViewMatrix);
            virtualObjectShader.setMat4("u_ModelViewProjection", modelViewProjectionMatrix);
            render.draw(virtualObjectMesh, virtualObjectShader, virtualSceneFrameBuffer);
        }

        // Compose the virtual scene with the background.
        float z_NEAR = 0.1f;
        float z_FAR = 100f;
        backgroundRenderer.drawVirtualScene(render, virtualSceneFrameBuffer, z_NEAR, z_FAR);
    }

    // Handle only one tap per frame, as taps are usually low frequency compared to frame rate.
    private void handleTap(Render render, Frame frame, Camera camera) {
        MotionEvent tap = queuedSingleTaps.poll();
        if (tap != null && camera.getTrackingState() == TrackingState.TRACKING) {
            List<HitResult> hitResultList;
            hitResultList = frame.hitTest(tap);
            for (HitResult hit : hitResultList) {
                // If any plane, Oriented Point, or Instant Placement Point was hit, create an anchor.
                Trackable trackable = hit.getTrackable();
                // If a plane was hit, check that it was hit inside the plane polygon.
                if ((trackable instanceof Plane
                        && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())
                        && (PlaneRenderer.calculateDistanceToPlane(hit.getHitPose(), camera.getPose()) > 0))
                        || (trackable instanceof Point
                        && ((Point) trackable).getOrientationMode()
                        == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                    // Cap the number of objects created. This avoids overloading both the
                    // rendering system and ARCore.

                    // My modification
                    if (plantAnchors.size() >= 20) {
                        toast.show();
                        plantAnchors.get(plantAnchors.size() - 1).getAnchor().detach();
                        plantAnchors.remove(plantAnchors.size() - 1);
                    }

                    // My addition
                    // Adds anchor to track position.
                    PlantAnchor p = new PlantAnchor(hit.createAnchor(), currentModel);
                    plantAnchors.add(p);
                    Pose anchorPose = p.getAnchor().getPose();
                    // Adds to plant positions
                    PlantPosition plantPosition = new PlantPosition(anchorPose.tx(), anchorPose.ty(), anchorPose.tz(), p.getType());
                    plantPositions.add(plantPosition);
                    break;
                }
            }
        }
    }

    /**
     * Checks if we detected at least one plane.
     */
    private boolean hasTrackingPlane() {
        for (Plane plane : session.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }

    private void configureSession() {
        Config config = session.getConfig();
        config.setLightEstimationMode(Config.LightEstimationMode.ENVIRONMENTAL_HDR);
        config.setInstantPlacementMode(Config.InstantPlacementMode.DISABLED);
        session.configure(config);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }

    /*
    * Interface method from SaveGardenDialog
    * Saves the garden details as long as the title is not empty
    * Performed within the SaveGardenDialog class
    * My addition
    */
    @Override
    public void SaveGarden(String gardenTitle) {
        designTitle = gardenTitle;
        if (!designTitle.isEmpty()) {
            gardenDesign.setTitle(designTitle);
            gardenDesign.setPlantPositions(plantPositions);
            RealmConfiguration config = new RealmConfiguration.Builder()
                    .allowQueriesOnUiThread(true)
                    .allowWritesOnUiThread(true)
                    .name("Garden")
                    .build();
            Realm realm = Realm.getInstance(config);

            realm.executeTransaction(transactionRealm -> {
                transactionRealm.insert(gardenDesign);
            });
            realm.close();

            CharSequence text = "Design Added";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getApplicationContext(), text, duration);
            toast.show();
        } else {
            CharSequence text = "Design not saved: Please add a title";
            int duration = Toast.LENGTH_SHORT;
            Toast toast = Toast.makeText(getApplicationContext(), text, duration);
            toast.show();
        }
    }
}
