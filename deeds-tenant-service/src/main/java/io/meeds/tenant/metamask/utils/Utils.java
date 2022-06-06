package io.meeds.tenant.metamask.utils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

public class Utils {

    public static Identity getIdentityByUsername(String name) {
        IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
        return identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, name);
    }
}
