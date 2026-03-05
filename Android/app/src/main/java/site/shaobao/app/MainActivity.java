package site.shaobao.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity{

    private ImageButton Bt_Index, Bt_2, Bt_3, Bt_4, Bt_5;
    private FragmentManager fragmentManager;
    private FragmentTransaction fragmentTransaction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Bt_Index = findViewById(R.id.fragment_b1);
        Bt_2 = findViewById(R.id.fragment_b2);
        Bt_3 = findViewById(R.id.fragment_b3);
        Bt_5 = findViewById(R.id.fragment_b5);

        Bt_Index.setOnClickListener(new MyClickListener());
        Bt_2.setOnClickListener(new MyClickListener());
        Bt_3.setOnClickListener(new MyClickListener());
        Bt_5.setOnClickListener(new MyClickListener());

        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(R.id.fragment_top, new SettingFragment()).commit();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Bt_Index = findViewById(R.id.fragment_b1);
        Bt_2 = findViewById(R.id.fragment_b2);
        Bt_3 = findViewById(R.id.fragment_b3);
        Bt_5 = findViewById(R.id.fragment_b5);

        Bt_Index.setOnClickListener(new MyClickListener());
        Bt_2.setOnClickListener(new MyClickListener());
        Bt_3.setOnClickListener(new MyClickListener());
        Bt_4.setOnClickListener(new MyClickListener());
        Bt_5.setOnClickListener(new MyClickListener());

        fragmentManager = getSupportFragmentManager();
        fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.add(R.id.fragment_top, new QuizFragment()).commit();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private class MyClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            fragmentTransaction = fragmentManager.beginTransaction();
            if (v.getId() == R.id.fragment_b1) {
                fragmentTransaction.replace(R.id.fragment_top, new QuizFragment());
            } else if (v.getId() == R.id.fragment_b2) {
                fragmentTransaction.replace(R.id.fragment_top, new UplodFragment());
            } else if (v.getId() == R.id.fragment_b3) {
                fragmentTransaction.replace(R.id.fragment_top, new FavoriteFragment());
            } else if (v.getId() == R.id.fragment_b5) {
                fragmentTransaction.replace(R.id.fragment_top, new SettingFragment());
            }
            fragmentTransaction.commit();
        }
    }

}
