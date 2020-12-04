package org.wikipedia.connectivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.robolectric.RobolectricTestRunner;

import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.eventplatform.EventPlatformClient;

import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
public class NetworkConnectivityReceiverTest {

    @Test
    public void testUpdatesEventPlatformClientEnabledStateOnUpdateOnlineState() {
        WikipediaApp app = WikipediaApp.getInstance();
        EventPlatformClient eventPlatformClient = mock(EventPlatformClient.class);
        app.setEventPlatformClient(eventPlatformClient);

        NetworkConnectivityReceiver networkConnectivityReceiver = new NetworkConnectivityReceiver();
        networkConnectivityReceiver.updateOnlineState();

        EventPlatformClient.setEnabled(anyBoolean());
    }

}
