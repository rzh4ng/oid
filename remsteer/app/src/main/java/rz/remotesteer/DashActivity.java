package rz.remotesteer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;

import com.rz4fun.remotesteer.R;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Vibrator;

public class DashActivity extends ActionBarActivity {

  @SuppressLint("NewApi")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_dash);
    status_textview_ = (TextView)findViewById(R.id.textView1);
    speed_textview_ = (TextView)findViewById(R.id.speed_textview);
    steering_wheel_imageview_ = (ImageView)findViewById(R.id.steer_image);
    speed_seekbar_ = (VerticalSeekBar)findViewById(R.id.speed_seekbar);
    engine_button_ = (ImageButton)findViewById(R.id.engine_button);
    final TextView d_textview = (TextView)findViewById(R.id.d_textview);
    final TextView r_textview = (TextView)findViewById(R.id.r_textview);
    // non UI variables
    vibrator_ = (Vibrator)this.getSystemService(Context.VIBRATOR_SERVICE);
    // Configuring the drive-reverse switch.
    /*
    drive_reverse_switch_ = (Switch)findViewById(R.id.drive_reverse_switch);
    drive_reverse_switch_.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
        if (isChecked) {
          drive_mode_ = DRIVE_BACKWARD;
          r_textview.setTextColor(Color.GREEN);
          d_textview.setTextColor(Color.GRAY);
        } else {
          drive_mode_ = DRIVE_FORWARD;
          r_textview.setTextColor(Color.GRAY);
          d_textview.setTextColor(Color.GREEN);
        }
      }
    });
    */
    // Configuring the engine start button.
    engine_button_.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (!engine_on_) {
          new EngineStarter().execute("Start");
        } else {
          ShutdownEngine();
        }
      }
    });
    // Configuring the speed controller.
    speed_seekbar_.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
      @Override
      public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
        SetSpeed(arg1);
      }

      @Override
      public void onStartTrackingTouch(SeekBar arg0) {
      }

      @Override
      public void onStopTrackingTouch(SeekBar arg0) {
        SetSpeed(SPEED_ZERO);
        arg0.setProgress(SPEED_ZERO);
      }
    });
    // Configuring the steering wheel.
    InitializeRotationMeter();
  }

  @Override
  public void onBackPressed() {
    ShutdownEngine();
    super.onBackPressed();
  }

  public void UISetEngineOn() {
    engine_on_ = true;
    engine_button_.setImageResource(R.drawable.b4_120_green);
    engine_button_.setEnabled(true);
  }

  public void ShutdownEngine() {
    try {
      if (socket_ != null && socket_.isConnected()) {
        command_writer_.write(COMMAND_OFF);
        command_writer_.flush();
        command_writer_.close();
        command_writer_ = null;
        socket_.close();
      }
      socket_ = null;
      engine_on_ = false;
      // UI updates
      engine_button_.setImageResource(R.drawable.b4_120_red);
      engine_button_.setEnabled(true);
      Toast.makeText(DashActivity.this, "Engine off", Toast.LENGTH_SHORT).show();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void SetSteer(int steer_angle) {
    if (engine_on_) {
      steering_wheel_imageview_.setRotation(steer_angle - 90);
      new VehicleController().execute(COMMAND_CATEGORY_STEER, steer_angle);
      Log.d("Remote Steer", "Steer: " + steer_angle);
    }
  }
  
  private void SetSpeed(int speed) {
    if (engine_on_) {
      // TODO: add UI updates
      new VehicleController().execute(COMMAND_CATEGORY_SPEED, speed);
      speed_textview_.setText((speed > SPEED_ZERO ? (speed - SPEED_ZERO) : (SPEED_ZERO - speed)) + "");
      Log.d("Remote Steer", " Speed: " + speed);
    }
  }
  
  
  private int InitializeRotationMeter() {
    steering_wheel_imageview_.setRotation(STEER_CENTER - 90);
    orientation_event_listener_ = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_GAME) {
      @SuppressLint("NewApi")
	  @Override
      public void onOrientationChanged(int orientation) {
        if (orientation == -1) {
          return;
        }
        // Note: with the current orientation, left-turn angles range for [180, 270); right-turn (270, 360].
        if (orientation < 180 || orientation > 360) {
          return;
        }
        SetSteer(orientation - 180);
      }
    };
    if (orientation_event_listener_.canDetectOrientation()){
      orientation_event_listener_.enable();
      return 0;
    } else{
      SetSteer(STEER_CENTER);
      return 1;
    }
  }
  
  
  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.dash, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private boolean engine_on_ = false;
  private OrientationEventListener orientation_event_listener_;
  private ImageView steering_wheel_imageview_;
  private VerticalSeekBar speed_seekbar_;
  private TextView status_textview_;
  private TextView speed_textview_;
  private ImageButton engine_button_;
  
  private Socket socket_;
  private PrintWriter command_writer_;
  
  private final String APPLICATION_TAG = "RemoteSteer";
  
  public static final int COMMAND_CATEGORY_SPEED = 1;
  public static final int COMMAND_CATEGORY_STEER = 2;
  public static final String COMMAND_OFF = "0#";

  public static final int STEER_CENTER = 90;

  public static final int SPEED_ZERO = 90;

  private Vibrator vibrator_;

  
  private class EngineStarter extends AsyncTask<String, Integer, String> {

	@Override
	protected String doInBackground(String... params) {
      if (socket_ == null) {
        socket_ = new Socket();
      }
      if (!socket_.isConnected()) {
        int count = 0;
        while (count++ < 3) {
          try {
            publishProgress(PROGRESS_START_ENGINE, count);
            socket_.connect(new InetSocketAddress("192.168.240.1", 5678), 3000);
            if (socket_.isConnected()) {
              command_writer_ =
                  new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket_.getOutputStream())), true);
              return "ON";
            }
          } catch (UnknownHostException e) {
            Log.d(APPLICATION_TAG, APPLICATION_TAG + e.getLocalizedMessage());
          } catch (IOException e) {
            Log.d(APPLICATION_TAG, APPLICATION_TAG + e.getLocalizedMessage());
          }
        }
      }
      return "OFF";
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
      if (progress[0] == PROGRESS_START_ENGINE) {
        engine_button_.setImageResource(R.drawable.b4_120_yellow);
      }
    }

    @Override
    protected void onPreExecute () {
      engine_button_.setEnabled(false);
      Toast.makeText(DashActivity.this, "Try starting engine", Toast.LENGTH_SHORT).show();
      vibrator_.vibrate(200);
    }

    @Override
    protected void onPostExecute (String result) {
      if (result == "ON") {
        UISetEngineOn();
      } else if (result == "OFF") {
        ShutdownEngine();
      }
      engine_button_.setEnabled(true);
    }

    private int PROGRESS_START_ENGINE = 1;
  }
  
  
  private class VehicleController extends AsyncTask<Integer, Void, Void> {
	@Override
	protected Void doInBackground(Integer... command) {
      if (command_writer_ == null) {
        return null;
      }
      int category = command[0];
      int value = command[1];
      if (category == COMMAND_CATEGORY_STEER) {
        command_writer_.write("S" + value + "#");
      } else if (category == COMMAND_CATEGORY_SPEED) {
        command_writer_.write("D" + value + "#");
      }
      command_writer_.flush();
      return null;
	}
  }
  
}
