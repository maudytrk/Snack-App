package id.maudy.snack_app;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

import id.maudy.snack_app.R;


public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Menampilkan HomeFragment secara default saat aplikasi pertama kali dijalankan
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        // Listener aksi klik pada item menu navigasi bawah
        bottomNavigationView.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    selectedFragment = new HomeFragment();
                } else if (id == R.id.nav_explore) {
                    selectedFragment = new ExploreFragment();
                } else if (id == R.id.nav_jual) {
                    selectedFragment = new JualFragment();
                } else if (id == R.id.nav_chat) {
                    selectedFragment = new ChatFragment();
                } else if (id == R.id.nav_profil) {
                    selectedFragment = new ProfilFragment();
                }

                return loadFragment(selectedFragment);
            }
        });
    }

    /**
     * Method helper untuk mengganti fragment di dalam FrameLayout
     */
    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            // Mengganti konten container dengan fragment baru
            fragmentTransaction.replace(R.id.fragment_container, fragment);
            fragmentTransaction.commit();
            return true;
        }
        return false;
    }
}