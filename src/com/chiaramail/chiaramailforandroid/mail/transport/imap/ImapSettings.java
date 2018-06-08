package com.chiaramail.chiaramailforandroid.mail.transport.imap;

import com.chiaramail.chiaramailforandroid.mail.store.ImapStore;
import com.chiaramail.chiaramailforandroid.mail.store.ImapStore.AuthType;
import com.chiaramail.chiaramailforandroid.mail.store.ImapStore.ImapConnection;

/**
 * Settings source for IMAP. Implemented in order to remove coupling between {@link ImapStore} and {@link ImapConnection}.
 */
public interface ImapSettings {
    String getHost();

    int getPort();

    int getConnectionSecurity();

    AuthType getAuthType();

    String getUsername();

    String getPassword();

    boolean useCompression(int type);

    String getPathPrefix();

    void setPathPrefix(String prefix);

    String getPathDelimeter();

    void setPathDelimeter(String delimeter);

    String getCombinedPrefix();

    void setCombinedPrefix(String prefix);
}
