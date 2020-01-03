package pl.edu.agh.emotionalrobot;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                stopEmotionService();
                finish();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        Intent intent = new Intent(getApplicationContext(), EmotionService.class);
        getApplicationContext().startService(intent);
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText("Engineering thesis project created by M. Cudak, A. Drewnik, J. Dutkiewicz.");
    }

    private void stopEmotionService() {
        getApplicationContext().stopService(new Intent(getApplicationContext(), EmotionService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopEmotionService();
    }
}

