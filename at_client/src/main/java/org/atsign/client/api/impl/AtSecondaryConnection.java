package org.atsign.client.api.impl;

import org.atsign.client.api.AtConnection;
import org.atsign.common.AtSign;

import java.io.IOException;

/**
 * A connection which understands how to talk with the secondary server.
 * @see org.atsign.client.api.AtConnection
 */
public class AtSecondaryConnection extends AtConnectionBase {
    private final AtSign atSign;
    public AtSign getAtSign() {return atSign;}

    public AtSecondaryConnection(AtSign atSign, String secondaryUrl, AtConnection.Authenticator authenticator, boolean autoReconnect, boolean logging) {
        super(secondaryUrl, authenticator, autoReconnect, logging);
        this.atSign = atSign;
    }

    @Override
    protected String parseRawResponse(String rawResponse) throws IOException {
        // Response can look like this:
        // @ prompt - or @<atSign>@ if connection has been authenticated
        // then either
        //   data:<stuff>
        //   error:<stuff>
        //   data:ok (for no-op requests)
        //   notification:<json> for notifications
        //
        // So:
        //   Find the first colon (exception if none)
        //   In what's before the colon
        //     Strip out the @alice@ or the single @
        int dataPos = rawResponse.indexOf("data:");
        int errorPos = rawResponse.indexOf("error:");
        int notificationPos = rawResponse.indexOf("notification:");
        if (dataPos >= 0) {
            return rawResponse.substring(dataPos);
        } else if (errorPos >= 0) {
            return rawResponse.substring(errorPos);
        } else if (notificationPos >= 0) {
            return rawResponse.substring(notificationPos);
        } else {
            throw new IOException("Invalid response from server: " + rawResponse);
        }
    }
}
