package br.org.basegame;

import android.graphics.Typeface;
import android.hardware.SensorManager;
import android.widget.Toast;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef.BodyType;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import org.andengine.audio.sound.Sound;
import org.andengine.audio.sound.SoundFactory;
import org.andengine.engine.camera.Camera;
import org.andengine.engine.options.EngineOptions;
import org.andengine.engine.options.ScreenOrientation;
import org.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.andengine.entity.primitive.Rectangle;
import org.andengine.entity.scene.IOnAreaTouchListener;
import org.andengine.entity.scene.IOnSceneTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.sprite.AnimatedSprite;
import org.andengine.entity.sprite.batch.DynamicSpriteBatch;
import org.andengine.entity.sprite.batch.SpriteBatch;
import org.andengine.entity.text.Text;
import org.andengine.entity.text.TextOptions;
import org.andengine.entity.util.FPSLogger;
import org.andengine.extension.physics.box2d.PhysicsConnector;
import org.andengine.extension.physics.box2d.PhysicsFactory;
import org.andengine.extension.physics.box2d.PhysicsWorld;
import org.andengine.extension.physics.box2d.util.Vector2Pool;
import org.andengine.input.sensor.acceleration.AccelerationData;
import org.andengine.input.sensor.acceleration.IAccelerationListener;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.FontFactory;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlasTextureRegionFactory;
import org.andengine.opengl.texture.region.TiledTextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.ui.activity.SimpleBaseGameActivity;
import org.andengine.util.HorizontalAlign;
import org.andengine.util.debug.Debug;

import java.io.IOException;

/**
 * (c) 2010 Nicolas Gramlich
 * (c) 2011 Zynga
 *
 * @author Nicolas Gramlich
 * @since 21:18:08 - 27.06.2010
 */
public class TesteActivity extends SimpleBaseGameActivity implements IAccelerationListener, IOnSceneTouchListener, IOnAreaTouchListener {
    // ===========================================================
    // Constants
    // ===========================================================

    private static final int CAMERA_WIDTH = 360;
    private static final int CAMERA_HEIGHT = 240;

    private static  float R  = 0.0f;

    private static  float G = 0.0f;
    private static  float B = 0.0f;

    // ===========================================================
    // Fields
    // ===========================================================

    private BitmapTextureAtlas mBitmapTextureAtlas;

    private TiledTextureRegion mBoxFaceTextureRegion;
    private TiledTextureRegion mCircleFaceTextureRegion;

    //private ITextureRegion mHoleTextureRegion;

    private int mFaceCount = 0;

    private PhysicsWorld mPhysicsWorld;

    private float mGravityX;
    private float mGravityY;

    private Scene mScene;

    private Font mFont;

    private Sound mExplosionSound;

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    public EngineOptions onCreateEngineOptions() {
        Toast.makeText(this, "Touch the screen to add objects. Touch an object to shoot it up into the air.", Toast.LENGTH_LONG).show();

        final Camera camera = new Camera(0, 0, CAMERA_WIDTH, CAMERA_HEIGHT);

        final EngineOptions engineOptions = new EngineOptions(true, ScreenOrientation.LANDSCAPE_FIXED, new RatioResolutionPolicy(CAMERA_WIDTH, CAMERA_HEIGHT), camera);

        engineOptions.getAudioOptions().setNeedsSound(true);

        return engineOptions;
    }

    @Override
    public void onCreateResources() {
        
        // Graphics

        BitmapTextureAtlasTextureRegionFactory.setAssetBasePath("gfx/");

        mBitmapTextureAtlas = new BitmapTextureAtlas(getTextureManager(), 64, 64, TextureOptions.BILINEAR);

        mBoxFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "face_box_tiled.png", 0, 0, 2, 1); // 64x32
        mCircleFaceTextureRegion = BitmapTextureAtlasTextureRegionFactory.createTiledFromAsset(mBitmapTextureAtlas, this, "face_circle_tiled.png", 0, 32, 2, 1); // 64x32

        //mHoleTextureRegion = BitmapTextureAtlasTextureRegionFactory.createFromAsset(mBitmapTextureAtlas, this, "hole.png", 0, 0);

        mBitmapTextureAtlas.load();

        mFont = FontFactory.create(getFontManager(), getTextureManager(), 256, 256, Typeface.create(Typeface.DEFAULT, Typeface.BOLD), 32);
        mFont.load();



        // Sounds

        SoundFactory.setAssetBasePath("mfx/");
        try {
            mExplosionSound = SoundFactory.createSoundFromAsset(mEngine.getSoundManager(), this, "teste.ogg");
        } catch (final IOException e) {
            Debug.e(e);
        }
    }

    @Override
    public Scene onCreateScene() {
        mEngine.registerUpdateHandler(new FPSLogger());

        mPhysicsWorld = new PhysicsWorld(new Vector2(0, SensorManager.GRAVITY_EARTH), false);

        mScene = new Scene();
        mScene.setBackground(new Background(0.09804f, 0.6274f, 0.8784f));
        mScene.setOnSceneTouchListener(this);

        //
        //final Sprite faceSprite1 = new Sprite(-50, 0, mHoleTextureRegion, getVertexBufferObjectManager());

        final SpriteBatch staticSpriteBatch = new SpriteBatch(mBitmapTextureAtlas, 2, getVertexBufferObjectManager());
        //staticSpriteBatch.draw(mHoleTextureRegion, -50, 0, mHoleTextureRegion.getWidth(), mHoleTextureRegion.getHeight(), 2, 2, 1, 1, 1, 1);

        staticSpriteBatch.submit();

        final SpriteBatch dynamicSpriteBatch = new DynamicSpriteBatch(mBitmapTextureAtlas, 2, getVertexBufferObjectManager()) {
            @Override
            public boolean onUpdateSpriteBatch() {
                //draw(faceSprite1);


                return true;
            }
        };

        staticSpriteBatch.setPosition(0, 0 + 50);
        mScene.attachChild(staticSpriteBatch);


        final VertexBufferObjectManager vertexBufferObjectManager = getVertexBufferObjectManager();
        final Rectangle ground = new Rectangle(0, CAMERA_HEIGHT - 2, CAMERA_WIDTH, 2, vertexBufferObjectManager);
        final Rectangle roof = new Rectangle(0, 0, CAMERA_WIDTH, 2, vertexBufferObjectManager);
        final Rectangle left = new Rectangle(0, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);
        final Rectangle right = new Rectangle(CAMERA_WIDTH - 2, 0, 2, CAMERA_HEIGHT, vertexBufferObjectManager);

        final FixtureDef wallFixtureDef = PhysicsFactory.createFixtureDef(0, 0.5f, 0.5f);
        PhysicsFactory.createBoxBody(mPhysicsWorld, ground, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(mPhysicsWorld, roof, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(mPhysicsWorld, left, BodyType.StaticBody, wallFixtureDef);
        PhysicsFactory.createBoxBody(mPhysicsWorld, right, BodyType.StaticBody, wallFixtureDef);

        mScene.attachChild(ground);
        mScene.attachChild(roof);
        mScene.attachChild(left);
        mScene.attachChild(right);

        final Text centerText = new Text(100, 40, mFont, "Hello AndEngine!\nYou can even have multilined text!", new TextOptions(HorizontalAlign.CENTER), vertexBufferObjectManager);
        final Text leftText = new Text(100, 170, mFont, "Also left aligned!\nLorem ipsum dolor sit amat...", new TextOptions(HorizontalAlign.LEFT), vertexBufferObjectManager);
        final Text rightText = new Text(100, 300, mFont, "And right aligned!\nLorem ipsum dolor sit amat...", new TextOptions(HorizontalAlign.RIGHT), vertexBufferObjectManager);

        mScene.attachChild(centerText);
        mScene.attachChild(leftText);
        mScene.attachChild(rightText);



        mScene.registerUpdateHandler(mPhysicsWorld);

        mScene.setOnAreaTouchListener(this);


        /*mScene.setOnAreaTouchListener(new IOnAreaTouchListener() {
            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final ITouchArea pTouchArea, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {

                return true;
            }
        });*/




        return mScene;
    }


    @Override
    public boolean onAreaTouched( final TouchEvent pSceneTouchEvent, final ITouchArea pTouchArea,final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
        if(pSceneTouchEvent.isActionDown()) {
            final AnimatedSprite face = (AnimatedSprite) pTouchArea;
            jumpFace(face);

            return true;
        }

        return false;
    }

    @Override
    public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
        if(mPhysicsWorld != null) {
            if(pSceneTouchEvent.isActionDown()) {
                addFace(pSceneTouchEvent.getX(), pSceneTouchEvent.getY());
                return true;
            }
        }
        return false;
    }

    @Override
    public void onAccelerationAccuracyChanged(final AccelerationData pAccelerationData) {

    }

    @Override
    public void onAccelerationChanged(final AccelerationData pAccelerationData) {
        mGravityX = pAccelerationData.getX();
        mGravityY = pAccelerationData.getY();

        final Vector2 gravity = Vector2Pool.obtain(mGravityX, mGravityY);
        mPhysicsWorld.setGravity(gravity);
        Vector2Pool.recycle(gravity);
    }

    @Override
    public void onResumeGame() {
        super.onResumeGame();

        enableAccelerationSensor(this);
    }

    @Override
    public void onPauseGame() {
        super.onPauseGame();

        disableAccelerationSensor();
    }

    // ===========================================================
    // Methods
    // ===========================================================

    private void addFace(final float pX, final float pY) {
        mFaceCount++;

        final AnimatedSprite face;
        final Body body;

        final FixtureDef objectFixtureDef = PhysicsFactory.createFixtureDef(1, 0.5f, 0.5f);

        if(mFaceCount % 2 == 1){
            face = new AnimatedSprite(pX, pY, mBoxFaceTextureRegion, getVertexBufferObjectManager());
            body = PhysicsFactory.createBoxBody(mPhysicsWorld, face, BodyType.DynamicBody, objectFixtureDef);
        } else {
            face = new AnimatedSprite(pX, pY, mCircleFaceTextureRegion, getVertexBufferObjectManager());
            body = PhysicsFactory.createCircleBody(mPhysicsWorld, face, BodyType.DynamicBody, objectFixtureDef);
        }

        mPhysicsWorld.registerPhysicsConnector(new PhysicsConnector(face, body, true, true));

        face.animate(new long[]{200,200}, 0, 1, true);
        face.setUserData(body);
        mScene.registerTouchArea(face);
        mScene.attachChild(face);
    }

    private void jumpFace(final AnimatedSprite face) {

        TesteActivity.this.mExplosionSound.play();

        final Body faceBody = (Body)face.getUserData();

        final Vector2 velocity = Vector2Pool.obtain(mGravityX * -50, mGravityY * -50);
        faceBody.setLinearVelocity(velocity);
        Vector2Pool.recycle(velocity);



        R = (float) Math.random();
        G = (float) Math.random();
        B = (float) Math.random();

        System.out.println(R + "\n  "+G +"\n  "+ B);

        mScene.setBackground(new Background(R, G, B));





    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
