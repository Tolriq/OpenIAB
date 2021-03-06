/*
 * Copyright 2012-2014 One Platform Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onepf.oms.appstore;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.onepf.oms.Appstore;
import org.onepf.oms.AppstoreInAppBillingService;
import org.onepf.oms.DefaultAppstore;
import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.nokiaUtils.NokiaSkuFormatException;
import org.onepf.oms.appstore.nokiaUtils.NokiaStoreHelper;
import org.onepf.oms.util.Logger;

import java.security.MessageDigest;
import java.util.Arrays;

public class NokiaStore extends DefaultAppstore {

    public static final String NOKIA_BILLING_PERMISSION = "com.nokia.payment.BILLING";
    private final Context context;

    @Nullable
    private NokiaStoreHelper billingService = null;

    //This is the expected SHA1 finger-print in HEX format
    private static final String EXPECTED_SHA1_FINGERPRINT = "C476A7D71C4CB92641A699C1F1CAC93CA81E0396";

    public static final String NOKIA_INSTALLER = "com.nokia.payment.iapenabler";
    public static final String VENDING_ACTION = "com.nokia.payment.iapenabler.InAppBillingService.BIND";

    public NokiaStore(final Context context) {
        Logger.i("NokiaStore.NokiaStore");

        this.context = context;
    }

    /**
     * Tells whether in-app billing is ready to work with specified app
     * For OpenStore app: if any in-app item for this app published in store
     */
    @Override
    public boolean isBillingAvailable(final String packageName) {
        Logger.i("NokiaStore.isBillingAvailable");
        Logger.d("packageName = ", packageName);

        return verifyFingerprint();
    }

    /**
     * Returns true only if actual installer for specified app
     */
    @Override
    public boolean isPackageInstaller(final String packageName) {
        Logger.d("sPackageInstaller: packageName = ", packageName);

        final PackageManager packageManager = context.getPackageManager();
        if (packageManager == null) {
            return false;
        }
        final String installerPackageName = packageManager.getInstallerPackageName(packageName);

        Logger.d("installerPackageName = ", installerPackageName);

        return NOKIA_INSTALLER.equals(installerPackageName);
    }

    @Override
    public int getPackageVersion(final String packageName) {
        Logger.d("getPackageVersion: packageName = " + packageName);
        return Appstore.PACKAGE_VERSION_UNDEFINED;
    }

    @Override
    public String getAppstoreName() {
        return OpenIabHelper.NAME_NOKIA;
    }

    @Nullable
    @Override
    public AppstoreInAppBillingService getInAppBillingService() {
        if (billingService == null) {
            billingService = new NokiaStoreHelper(context, this);
        }
        return billingService;
    }

    /**
     * Checks SHA1 fingerprint of the enabler
     *
     * @return true if signature matches, false if package is not found or signature does not match.
     */
    private boolean verifyFingerprint() {
        try {
            PackageManager pm = context.getPackageManager();
            if (pm == null) {
                return false;
            }

            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo info = pm.getPackageInfo(NOKIA_INSTALLER, PackageManager.GET_SIGNATURES);

            if (info.signatures != null && info.signatures.length == 1) {
                byte[] cert = info.signatures[0].toByteArray();
                MessageDigest digest;
                digest = MessageDigest.getInstance("SHA1");
                byte[] ENABLER_FINGERPRINT = digest.digest(cert);
                byte[] EXPECTED_FINGERPRINT = hexStringToByteArray(EXPECTED_SHA1_FINGERPRINT);

                if (Arrays.equals(ENABLER_FINGERPRINT, EXPECTED_FINGERPRINT)) {
                    Logger.i("isBillingAvailable", "NIAP signature verified");
                    return true;
                }
            }
        } catch (Exception ignore) {
        }
        return false;
    }

    /**
     * Validate item SKU in Nokia Store.
     * Nokia store item's SKU can contain only digits.
     *
     * @param sku SKU for validate.
     * @throws org.onepf.oms.appstore.nokiaUtils.NokiaSkuFormatException If sku in wrong format for Nokia Store.
     */
    public static void checkSku(@NonNull String sku) {
        if (!TextUtils.isDigitsOnly(sku)) {
            throw new NokiaSkuFormatException();
        }
    }

    @NonNull
    private static byte[] hexStringToByteArray(@NonNull String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
