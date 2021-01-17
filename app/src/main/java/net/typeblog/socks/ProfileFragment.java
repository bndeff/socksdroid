package net.typeblog.socks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.ListPreference;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import net.typeblog.socks.util.Profile;
import net.typeblog.socks.util.ProfileManager;
import net.typeblog.socks.util.Utility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static net.typeblog.socks.util.Constants.*;

public class ProfileFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener,
        CompoundButton.OnCheckedChangeListener {
    private ProfileManager mManager;
    private Profile mProfile;

    private Switch mSwitch;
    private boolean mRunning = false;
    private boolean mStarting = false, mStopping = false;
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName p1, IBinder binder) {
            mBinder = IVpnService.Stub.asInterface(binder);

            try {
                mRunning = mBinder.isRunning();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (mRunning) {
                updateState();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName p1) {
            mBinder = null;
        }
    };
    private final Runnable mStateRunnable = new Runnable() {
        @Override
        public void run() {
            updateState();
            mSwitch.postDelayed(this, 1000);
        }
    };
    private IVpnService mBinder;

    private ListPreference mPrefProfile, mPrefRoutes;
    private EditTextPreference mPrefServer;
    private EditTextPreference mPrefPort;
    private EditTextPreference mPrefUsername;
    private EditTextPreference mPrefPassword;
    private EditTextPreference mPrefDns;
    private EditTextPreference mPrefDnsPort;
    private MultiSelectListPreference mPrefAppList;
    private EditTextPreference mPrefUDPGW;
    private CheckBoxPreference mPrefUserpw, mPrefPerApp, mPrefAppBypass, mPrefIPv6, mPrefUDP, mPrefAuto;
    private Context context;

    public void setContext(Context context) {
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.settings);
        setHasOptionsMenu(true);
        mManager = new ProfileManager(getActivity().getApplicationContext());
        initPreferences();
        reload();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.main, menu);

        MenuItem s = menu.findItem(R.id.switch_main);
        mSwitch = s.getActionView().findViewById(R.id.switch_action_button);
        mSwitch.setOnCheckedChangeListener(this);
        mSwitch.postDelayed(mStateRunnable, 1000);
        checkState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.prof_add:
                addProfile();
                return true;
            case R.id.prof_del:
                removeProfile();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onPreferenceClick(Preference p) {
        // TODO: Implement this method
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference p, Object newValue) {
        if (p == mPrefProfile) {
            String name = newValue.toString();
            mProfile = mManager.getProfile(name);
            mManager.switchDefault(name);
            reload();
            return true;
        } else if (p == mPrefServer) {
            mProfile.setServer(newValue.toString());
            resetTextN(mPrefServer, newValue);
            return true;
        } else if (p == mPrefPort) {
            if (TextUtils.isEmpty(newValue.toString()))
                return false;

            mProfile.setPort(Integer.parseInt(newValue.toString()));
            resetTextN(mPrefPort, newValue);
            return true;
        } else if (p == mPrefUserpw) {
            mProfile.setIsUserpw(Boolean.parseBoolean(newValue.toString()));
            return true;
        } else if (p == mPrefUsername) {
            mProfile.setUsername(newValue.toString());
            resetTextN(mPrefUsername, newValue);
            return true;
        } else if (p == mPrefPassword) {
            mProfile.setPassword(newValue.toString());
            resetTextN(mPrefPassword, newValue);
            return true;
        } else if (p == mPrefRoutes) {
            mProfile.setRoute(newValue.toString());
            resetListN(mPrefRoutes, newValue);
            return true;
        } else if (p == mPrefDns) {
            mProfile.setDns(newValue.toString());
            resetTextN(mPrefDns, newValue);
            return true;
        } else if (p == mPrefDnsPort) {
            if (TextUtils.isEmpty(newValue.toString()))
                return false;

            mProfile.setDnsPort(Integer.valueOf(newValue.toString()));
            resetTextN(mPrefDnsPort, newValue);
            return true;
        } else if (p == mPrefPerApp) {
            mProfile.setIsPerApp(Boolean.parseBoolean(newValue.toString()));
            return true;
        } else if (p == mPrefAppBypass) {
            mProfile.setIsBypassApp(Boolean.parseBoolean(newValue.toString()));
            return true;
        } else if (p == mPrefAppList) {
            List<String> newValues = new ArrayList<>((HashSet<String>) newValue);
            String appList = TextUtils.join("\n", newValues);
            mProfile.setAppList(appList);
            updateAppList();
            Log.d("setAppList", "appList:\n" + mProfile.getAppList());
            return true;
        } else if (p == mPrefIPv6) {
            mProfile.setHasIPv6(Boolean.parseBoolean(newValue.toString()));
            return true;
        } else if (p == mPrefUDP) {
            mProfile.setHasUDP(Boolean.parseBoolean(newValue.toString()));
            return true;
        } else if (p == mPrefUDPGW) {
            mProfile.setUDPGW(newValue.toString());
            resetTextN(mPrefUDPGW, newValue);
            return true;
        } else if (p == mPrefAuto) {
            mProfile.setAutoConnect(Boolean.parseBoolean(newValue.toString()));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton p1, boolean checked) {
        if (checked) {
            startVpn();
        } else {
            stopVpn();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            Utility.startVpn(getActivity(), mProfile);
            checkState();
        }
    }

    private void initPreferences() {
        mPrefProfile = (ListPreference) findPreference(PREF_PROFILE);
        mPrefServer = (EditTextPreference) findPreference(PREF_SERVER_IP);
        mPrefPort = (EditTextPreference) findPreference(PREF_SERVER_PORT);
        mPrefUserpw = (CheckBoxPreference) findPreference(PREF_AUTH_USERPW);
        mPrefUsername = (EditTextPreference) findPreference(PREF_AUTH_USERNAME);
        mPrefPassword = (EditTextPreference) findPreference(PREF_AUTH_PASSWORD);
        mPrefRoutes = (ListPreference) findPreference(PREF_ADV_ROUTE);
        mPrefDns = (EditTextPreference) findPreference(PREF_ADV_DNS);
        mPrefDnsPort = (EditTextPreference) findPreference(PREF_ADV_DNS_PORT);
        mPrefPerApp = (CheckBoxPreference) findPreference(PREF_ADV_PER_APP);
        mPrefAppBypass = (CheckBoxPreference) findPreference(PREF_ADV_APP_BYPASS);
        mPrefAppList = (MultiSelectListPreference) findPreference(PREF_ADV_APP_LIST);
        mPrefIPv6 = (CheckBoxPreference) findPreference(PREF_IPV6_PROXY);
        mPrefUDP = (CheckBoxPreference) findPreference(PREF_UDP_PROXY);
        mPrefUDPGW = (EditTextPreference) findPreference(PREF_UDP_GW);
        mPrefAuto = (CheckBoxPreference) findPreference(PREF_ADV_AUTO_CONNECT);

        mPrefProfile.setOnPreferenceChangeListener(this);
        mPrefServer.setOnPreferenceChangeListener(this);
        mPrefPort.setOnPreferenceChangeListener(this);
        mPrefUserpw.setOnPreferenceChangeListener(this);
        mPrefUsername.setOnPreferenceChangeListener(this);
        mPrefPassword.setOnPreferenceChangeListener(this);
        mPrefRoutes.setOnPreferenceChangeListener(this);
        mPrefDns.setOnPreferenceChangeListener(this);
        mPrefDnsPort.setOnPreferenceChangeListener(this);
        mPrefPerApp.setOnPreferenceChangeListener(this);
        mPrefAppBypass.setOnPreferenceChangeListener(this);
        mPrefAppList.setOnPreferenceChangeListener(this);
        mPrefIPv6.setOnPreferenceChangeListener(this);
        mPrefUDP.setOnPreferenceChangeListener(this);
        mPrefUDPGW.setOnPreferenceChangeListener(this);
        mPrefAuto.setOnPreferenceChangeListener(this);
    }

    private void reload() {
        if (mProfile == null) {
            mProfile = mManager.getDefault();
        }

        mPrefProfile.setEntries(mManager.getProfiles());
        mPrefProfile.setEntryValues(mManager.getProfiles());
        mPrefProfile.setValue(mProfile.getName());
        mPrefRoutes.setValue(mProfile.getRoute());
        resetList(mPrefProfile, mPrefRoutes);

        mPrefUserpw.setChecked(mProfile.isUserPw());
        mPrefPerApp.setChecked(mProfile.isPerApp());
        mPrefAppBypass.setChecked(mProfile.isBypassApp());
        mPrefIPv6.setChecked(mProfile.hasIPv6());
        mPrefUDP.setChecked(mProfile.hasUDP());
        mPrefAuto.setChecked(mProfile.autoConnect());

        mPrefServer.setText(mProfile.getServer());
        mPrefPort.setText(String.valueOf(mProfile.getPort()));
        mPrefUsername.setText(mProfile.getUsername());
        mPrefPassword.setText(mProfile.getPassword());
        mPrefDns.setText(mProfile.getDns());
        mPrefDnsPort.setText(String.valueOf(mProfile.getDnsPort()));
        mPrefUDPGW.setText(mProfile.getUDPGW());
        resetText(mPrefServer, mPrefPort, mPrefUsername, mPrefPassword, mPrefDns, mPrefDnsPort, mPrefUDPGW);

        updateAppList();
    }

    private void updateAppList() {
        HashSet<String> selectedApps = new HashSet<String>(Arrays.asList(mProfile.getAppList().split("\n")));
        List<String> selectedAndExistsApps = new ArrayList<String>();

        Map<String, String> packages = getPackages();
        CharSequence[] titles = new CharSequence[packages.size()];
        CharSequence[] packageNames = new CharSequence[packages.size()];

        //--------------- Sort the application list ---------------
        int i = 0;

        // First add the selected application so that
        // the selected application will appear in the front
        for (Map.Entry<String, String> entry : packages.entrySet()) {
            if (selectedApps.contains(entry.getValue())) {
                selectedAndExistsApps.add(entry.getValue());
                packageNames[i] = entry.getValue();
                titles[i] = entry.getKey();
                i++;
            }
        }
        // Then add unselected apps
        for (Map.Entry<String, String> entry : packages.entrySet()) {
            if (!selectedApps.contains(entry.getValue())) {
                packageNames[i] = entry.getValue();
                titles[i] = entry.getKey();
                i++;
            }
        }

        mPrefAppList.setEntries(titles);
        mPrefAppList.setEntryValues(packageNames);

        // Update the stored AppList (non-existent applications was deleted)
        mProfile.setAppList(TextUtils.join("\n", selectedAndExistsApps));
    }

    private Map<String, String> getPackages() {
        Map<String, String> packages = new TreeMap<String, String>();
        try {
            String myself = context.getApplicationInfo().packageName;
            List<PackageInfo> packageInfos = context.getPackageManager().getInstalledPackages(0);

            // Check if multiple applications have the same name
            Map<String, Integer> nameCount = new HashMap<String, Integer>();
            for (PackageInfo info : packageInfos) {
                String appName = info.applicationInfo.loadLabel(context.getPackageManager()).toString();
                if (nameCount.containsKey(appName)) {
                    nameCount.put(appName, nameCount.get(appName) + 1);
                } else {
                    nameCount.put(appName, 1);
                }
            }

            for (PackageInfo info : packageInfos) {
                String appName = info.applicationInfo.loadLabel(context.getPackageManager()).toString();
                String packageName = info.packageName;
                if (!myself.equals(packageName)) {
                    // If the name is duplicated, automatically add the package name as a suffix
                    if (nameCount.get(appName) > 1) {
                        appName = appName + " (" + packageName + ")";
                    }
                    packages.put(appName, packageName);
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();;
        }
        return packages;
    }

    private void resetList(ListPreference... pref) {
        for (ListPreference p : pref)
            p.setSummary(p.getEntry());
    }

    private void resetListN(ListPreference pref, Object newValue) {
        pref.setSummary(newValue.toString());
    }

    private void resetText(EditTextPreference... pref) {
        for (EditTextPreference p : pref) {
            if ((p.getEditText().getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) != InputType.TYPE_TEXT_VARIATION_PASSWORD) {
                p.setSummary(p.getText());
            } else {
                if (p.getText().length() > 0)
                    p.setSummary(String.format(Locale.US,
                            String.format(Locale.US, "%%0%dd", p.getText().length()), 0)
                            .replace("0", "*"));
                else
                    p.setSummary("");
            }
        }
    }

    private void resetTextN(EditTextPreference pref, Object newValue) {
        if ((pref.getEditText().getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) != InputType.TYPE_TEXT_VARIATION_PASSWORD) {
            pref.setSummary(newValue.toString());
        } else {
            String text = newValue.toString();
            if (text.length() > 0)
                pref.setSummary(String.format(Locale.US,
                        String.format(Locale.US, "%%0%dd", text.length()), 0)
                        .replace("0", "*"));
            else
                pref.setSummary("");
        }
    }

    private void addProfile() {
        final EditText e = new EditText(getActivity());
        e.setSingleLine(true);

        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.prof_add)
                .setView(e)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String name = e.getText().toString().trim();

                        if (!TextUtils.isEmpty(name)) {
                            Profile p = mManager.addProfile(name);

                            if (p != null) {
                                mProfile = p;
                                reload();
                                return;
                            }
                        }

                        Toast.makeText(getActivity(),
                                String.format(getString(R.string.err_add_prof), name),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {

                    }
                })
                .create().show();
    }

    private void removeProfile() {
        new AlertDialog.Builder(getActivity())
                .setTitle(R.string.prof_del)
                .setMessage(String.format(getString(R.string.prof_del_confirm), mProfile.getName()))
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        if (!mManager.removeProfile(mProfile.getName())) {
                            Toast.makeText(getActivity(),
                                    getString(R.string.err_del_prof, mProfile.getName()),
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            mProfile = mManager.getDefault();
                            reload();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {

                    }
                })
                .create().show();
    }

    private void checkState() {
        mRunning = false;
        mSwitch.setEnabled(false);
        mSwitch.setOnCheckedChangeListener(null);

        if (mBinder == null) {
            getActivity().bindService(new Intent(getActivity(), SocksVpnService.class), mConnection, 0);
        }
    }

    private void updateState() {
        if (mBinder == null) {
            mRunning = false;
        } else {
            try {
                mRunning = mBinder.isRunning();
            } catch (Exception e) {
                mRunning = false;
            }
        }

        mSwitch.setChecked(mRunning);

        if ((!mStarting && !mStopping) || (mStarting && mRunning) || (mStopping && !mRunning)) {
            mSwitch.setEnabled(true);
        }

        if (mStarting && mRunning) {
            mStarting = false;
        }

        if (mStopping && !mRunning) {
            mStopping = false;
        }

        mSwitch.setOnCheckedChangeListener(ProfileFragment.this);
    }

    private void startVpn() {
        mStarting = true;
        Intent i = VpnService.prepare(getActivity());

        if (i != null) {
            startActivityForResult(i, 0);
        } else {
            onActivityResult(0, Activity.RESULT_OK, null);
        }
    }

    private void stopVpn() {
        if (mBinder == null)
            return;

        mStopping = true;

        try {
            mBinder.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mBinder = null;

        getActivity().unbindService(mConnection);
        checkState();
    }
}
