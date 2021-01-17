package net.typeblog.socks;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ProfileFragment fragment = new ProfileFragment();
        fragment.setContext(getApplicationContext());
        this.getFragmentManager().beginTransaction().replace(android.R.id.content, fragment).commit();
    }
}
