package com.beltaief.reactivefb.actions;

import com.beltaief.reactivefb.ReactiveFB;
import com.beltaief.reactivefb.SessionManager;
import com.beltaief.reactivefb.SimpleFacebookConfiguration;
import com.beltaief.reactivefb.util.PermissionHelper;
import com.facebook.login.LoginResult;
import java.util.List;
import io.reactivex.MaybeEmitter;
import io.reactivex.MaybeOnSubscribe;

/**
 * Created by wassim on 9/21/16.
 */

class AdditionalPermissionOnSubscribe implements MaybeOnSubscribe<LoginResult> {

    private SessionManager sessionManager;
    private List<PermissionHelper> mPermissions;

    AdditionalPermissionOnSubscribe(List<PermissionHelper> permissions) {
        sessionManager = ReactiveFB.getSessionManager();
        mPermissions = permissions;
    }

    @Override
    public void subscribe(MaybeEmitter<LoginResult> emitter) throws Exception {
        SimpleFacebookConfiguration mConfiguration = ReactiveFB.getConfiguration();

        int flag = mConfiguration.getType(mPermissions);
        sessionManager.setCallbackAsLogin(emitter);

        switch (flag) {
            case 1:
                sessionManager.requestReadPermissions(PermissionHelper.convert(mPermissions));
                break;
            case 3:
                // in case of marking in configuration the option of getting publish permission,
                // just after read permissions
                if (mConfiguration.isAllPermissionsAtOnce()) {
                    sessionManager.getLoginCallback().askPublishPermissions = true;
                    sessionManager.getLoginCallback().publishPermissions =
                            PermissionHelper.fetchPermissions(mPermissions, PermissionHelper.Type.PUBLISH);
                }
                sessionManager.requestReadPermissions(
                        PermissionHelper.fetchPermissions(mPermissions, PermissionHelper.Type.READ));
                break;
            case 2:
                sessionManager.requestPublishPermissions(PermissionHelper.convert(mPermissions));
                break;
        }
    }
}
