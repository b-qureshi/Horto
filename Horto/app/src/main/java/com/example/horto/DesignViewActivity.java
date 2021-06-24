package com.example.horto;

/* Class based upon the ArDesignActivity class, therefore also containing reused code.
   Class constructed from multiple classes from the sample project:
        - CameraPermissionHelper
        - DepthSettings
        - DisplayRotationHelper
        - FullScreenHelper
        - TapHelper
        - HelloArActivity


   Link to sample project: https://github.com/google-ar/arcore-Android-sdk.git
   All modifications made over duration of the project.
   * The comment: 'My addition' has been made to the areas I have added *
 */

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.Image;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.horto.common.GardenDesign;
import com.example.horto.common.Plant;
import com.example.horto.common.PlantPosition;
import com.example.horto.common.android_helpers.SnackbarHelper;
import com.example.horto.common.android_helpers.TrackingStateHelper;
import com.example.horto.common.arcore.BackgroundRenderer;
import com.example.horto.common.arcore.PlaneRenderer;
import com.example.horto.common.arcore.PlantAnchor;
import com.example.horto.common.opengl.Framebuffer;
import com.example.horto.common.opengl.Mesh;
import com.example.horto.common.opengl.Render;
import com.example.horto.common.opengl.Shader;
import com.example.horto.common.opengl.VertexBuffer;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Camera;
import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingFailureReason;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.CameraNotAvailableException;
import com.google.ar.core.exceptions.NotYetAvailableException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;

import org.bson.types.ObjectId;

import java.io.IOException;
import java.util.ArrayList;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmList;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class DesignViewActivity extends AppCompatActivity implements Render.Renderer {

    private static final String TAG = ArDesignActivity.class.getSimpleName();
    private GLSurfaceView glSurfaceView;
    private Session session;

    private static final float APPROXIMATE_DISTANCE_METERS = 2.0f;

    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private final TrackingStateHelper trackingStateHelper = new TrackingStateHelper(this);

    // Rendering Objects
    private PlaneRenderer planeRenderer;
    private BackgroundRenderer backgroundRenderer;
    private Framebuffer virtualSceneFrameBuffer;
    private boolean hasSetTextureNames = false;

    // Depth settings
    public static final String SHARED_PREFERENCES_ID_DEPTH = "SHARED_PREFERENCES_OCCLUSION_OPTIONS";
    public static final String SHARED_PREFERENCES_USE_DEPTH_FOR_OCCLUSION = "use_depth_for_occlusion";
    private boolean useDepthForOcclusion = false;
    private SharedPreferences sharedPreferencesDepth;

    // Point Cloud
    private VertexBuffer pointCloudVertexBuffer;
    private Mesh pointCloudMesh;
    private Shader pointCloudShader;

    private long lastPointCloudTimestamp = 0;

    // Virtual object (Plant)
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

    private ArrayList<PlantPosition> plantPositions;

    private boolean modelsAdded;

    private CharSequence text = "Design has been deleted";
    private int duration = Toast.LENGTH_SHORT;
    Toast toast;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.design_ar_view);
        glSurfaceView = findViewById(R.id.view_design_ar);

        // Add model paths to list
        // My addition
        modelPaths.add("models/CoffeePlant.obj");
        modelPaths.add("models/Bromeliads.obj");
        modelPaths.add("models/dandelion.obj");
        modelPaths.add("models/FiddleleafFigPottedPlant.obj");

        Render render = new Render(glSurfaceView, this, getAssets());
        display = this.getDisplay();

        // Depth Settings Initialise
        sharedPreferencesDepth = this.getSharedPreferences(SHARED_PREFERENCES_ID_DEPTH, Context.MODE_PRIVATE);
        useDepthForOcclusion =
                sharedPreferencesDepth.getBoolean(SHARED_PREFERENCES_USE_DEPTH_FOR_OCCLUSION, false);


        // My addition
        String designIDString = getIntent().getStringExtra("designID");
        ObjectId designid = new ObjectId(designIDString);

        // Get design properties from database, based upon the ID passed in
        // from the previous page intent
        // My addition
        RealmConfiguration config = new RealmConfiguration.Builder()
                .allowQueriesOnUiThread(true)
                .allowWritesOnUiThread(true)
                .name("Garden")
                .build();
        Realm realm = Realm.getInstance(config);
        RealmQuery realmQuery = realm.where(GardenDesign.class);
        GardenDesign designResult = (GardenDesign) realmQuery.equalTo("_id", designid).findFirst();
        Log.println(Log.INFO,"DESIGN_VIEW", designResult.toString());

        // Positions copied over into list
        // My addition
        RealmList<PlantPosition> positionResults = designResult.getPlantPositions();
        plantPositions = (ArrayList<PlantPosition>) realm.copyFromRealm(positionResults);

        // Back button to go to home screen
        // My addition
        Button backButton = findViewById(R.id.back_design_ar_button);
        backButton.setOnClickListener(v -> {
            Intent myIntent = new Intent(v.getContext(), DesignListActivity.class);
            startActivityForResult(myIntent, 0);
            DesignViewActivity.this.finish();
            realm.close();
        });

        //  Delete garden design from list button
        // My addition
        Button deleteButton = findViewById(R.id.delete_design_button);
        deleteButton.setOnClickListener(v -> {
            // Create dialog for the save button
            AlertDialog.Builder builder = new AlertDialog.Builder(DesignViewActivity.this);

            builder.setPositiveButton("Delete", (dialog, id) -> {
                realm.executeTransaction(realm1 -> {
                    RealmResults<GardenDesign> results = realm1.where(GardenDesign.class).equalTo("_id", designid).findAll();
                    results.deleteFirstFromRealm();
                });

                toast = Toast.makeText(DesignViewActivity.this, text, duration);
                toast.show();

                Intent myIntent = new Intent(v.getContext(), DesignListActivity.class);
                startActivityForResult(myIntent, 0);

                DesignViewActivity.this.finish();

                // Close the database connection
                realm.close();
            });
            builder.setNegativeButton("Cancel", (dialog, id) -> dialog.dismiss());

            builder.setTitle("Are you sure you want to delete this design?");
            AlertDialog dialog = builder.create();
            dialog.show();
        });
        // My addition
        modelsAdded = false;

    }

    @Override
    protected void onDestroy() {
        if (session != null) {
            session.close();
            session = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session == null) {
            Exception exception = null;
            String message = null;
            try {
                session = new Session(this);
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
    }

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

            // Create meshes from the modelPaths list
            // My addition
            for (String path : modelPaths) {
                Mesh mesh = Mesh.createFromAsset(render, path);
                meshes.add(mesh);
            }


            // Set mesh to the first element mesh list
            virtualObjectMesh = meshes.get(0);


            // Create shader object from shaders.
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

        try {
            session.resume();
        } catch (CameraNotAvailableException e) {
            e.printStackTrace();
        }

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
        // Keep the screen unlocked while tracking, but allow it to lock when tracking stops.
        trackingStateHelper.updateKeepScreenOnFlag(camera.getTrackingState());

        // Show a message based on whether tracking has failed, if planes are detected, and if the user
        // has placed any objects.
        String message = null;
        if (camera.getTrackingState() == TrackingState.PAUSED) {
            if (camera.getTrackingFailureReason() == TrackingFailureReason.NONE) {
                message = "Looking for surfaces...";
            } else {
                message = TrackingStateHelper.getTrackingFailureReasonString(camera);
            }
        } else {
            message = "Looking for surfaces...";
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

        //My addition
        // Add models to world once, prevent being redrawn.
        if (!modelsAdded){
            addModels();
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
        for (PlantAnchor anchor : plantAnchors) {
            if (anchor.getAnchor().getTrackingState() != TrackingState.TRACKING) {
                continue;
            }
            // My addition
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

    // My addition
    // Create anchors from plant positions.
    public void addModels(){
        for (PlantPosition p : plantPositions){
            float pos[] = new float[3];
            pos[0] = p.getX();
            pos[1] = p.getY();
            pos[2] = p.getZ();
            float rot[] = new float[4];
            Anchor anchor = session.createAnchor(new Pose(pos, rot));
            PlantAnchor pa = new PlantAnchor(anchor, p.getPlantName());
            plantAnchors.add(pa);
        }

        // Confirms models have been added.
        modelsAdded = true;
    }


}
