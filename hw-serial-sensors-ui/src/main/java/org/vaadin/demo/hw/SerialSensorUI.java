package org.vaadin.demo.hw;

import com.vaadin.annotations.JavaScript;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.annotations.Widgetset;
import com.vaadin.server.Page;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;
import elemental.json.Json;
import elemental.json.JsonObject;
import elemental.json.JsonType;
import elemental.json.JsonValue;

import javax.servlet.annotation.WebServlet;
import java.util.function.BiConsumer;

import static com.vaadin.ui.Notification.Type.WARNING_MESSAGE;

/**
 *
 */
@Theme("valo")
@JavaScript("vaadinSerial.js")
@Widgetset("org.vaadin.demo.hw.SerialSensorWidgetset")
public class SerialSensorUI extends UI {

    /**
     * Provides various helper methods for connectors. Meant for internal use.
     *
     * @author Vaadin Ltd
     */
    public interface DataChart {
        Component getComponent();

        void processLine(String line);
    }

    private Serial serial;
    private Button connectButton;
    private Button disconnectButton;

    private DataChart chartHandler;

    @Override
    protected void init(VaadinRequest vaadinRequest) {
        chartHandler = new DataChartImpl();
        serial = new Serial();
        serial.setReceiveHandler((success, info) -> {
            if (success) {
                chartHandler.processLine(info);
            } else {
                Notification.show(info, WARNING_MESSAGE);
            }
        });
        serial.setStatusHandler((connected, message) -> getUI().access(() -> {
                    connectButton.setEnabled(!connected);
                    disconnectButton.setEnabled(connected);
                    if (message != null && !message.isEmpty()) {
                        Notification.show(message, WARNING_MESSAGE);
                    }
                }
        ));
        final VerticalLayout layout = new VerticalLayout();

        connectButton = new Button("Connect", e -> {
            serial.connect();
            connectButton.setEnabled(false);
        });
        connectButton.addStyleName(ValoTheme.BUTTON_SMALL);

        disconnectButton = new Button("Disconnect", e -> {
            serial.disconnect();
            disconnectButton.setEnabled(false);
        });

        disconnectButton.setEnabled(false);
        disconnectButton.addStyleName(ValoTheme.BUTTON_SMALL);
        disconnectButton.addStyleName(ValoTheme.BUTTON_DANGER);

        HorizontalLayout buttonLayout = new HorizontalLayout(connectButton, disconnectButton);
        buttonLayout.setSpacing(true);

        Component chart = chartHandler.getComponent();
        chart.setSizeFull();

        layout.addComponents(buttonLayout, chart);
        layout.setExpandRatio(chart, 1.0f);
        layout.setSizeFull();
        setContent(layout);
    }


    @WebServlet(urlPatterns = "/*", name = "SerialSensorServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = SerialSensorUI.class, productionMode = false)
    public static class SerialSensorServlet extends VaadinServlet {
    }

    public class Serial {

        private BiConsumer<Boolean, String> textLineHandler;
        private BiConsumer<Boolean, String> connectionHandler;

        private Serial() {
            Page.getCurrent().getJavaScript().addFunction("org.vaadin.demo.hw.serialCallBack",
                    arguments -> {
                        JsonObject jsonObject = (arguments).get(0);
                        String type = getJsonString(jsonObject, "type");
                        String message = getJsonString(jsonObject, "message");
                        String data = getJsonString(jsonObject, "data");
                        boolean success = getJsonBoolean(jsonObject, "success", true);
                        switch (type) {
                            case "connect":
                                connectionHandler.accept(success, message);
                                break;
                            case "disconnect":
                                connectionHandler.accept(false, null);
                                break;
                            case "readSerial":
                                textLineHandler.accept(success, success ? data : message);
                                break;
                        }
                        if (!message.isEmpty()) {
                            Notification.show(message, success ? Notification.Type.ASSISTIVE_NOTIFICATION : WARNING_MESSAGE);
                        } else if (!success) {
                            Notification.show("Unknown error", WARNING_MESSAGE);
                        }
                    });
        }

        private String getJsonString(JsonObject jsonObject, String key) {
            return getJsonString(jsonObject, key, "");
        }

        private String getJsonString(JsonObject jsonObject, String key, String defValue) {
            JsonValue jsonValue = jsonObject.get(key);
            if (jsonValue == null || jsonValue.getType() == JsonType.NULL) return defValue;
            return jsonValue.asString();
        }

        private boolean getJsonBoolean(JsonObject jsonObject, String key, boolean defValue) {
            return jsonObject.hasKey(key) ? jsonObject.getBoolean(key) : defValue;
        }

        public void connect() {
            Page.getCurrent().getJavaScript().execute("connect()");
        }

        public void disconnect() {
            Page.getCurrent().getJavaScript().execute("disconnect()");
        }

        @SuppressWarnings("unused")
        public boolean sendTextToSerial(String text) {
            String escapedText = Json.create(text).toJson();
            Page.getCurrent().getJavaScript().execute("sendText(" + escapedText + ")");
            return false;
        }

        public void setReceiveHandler(BiConsumer<Boolean, String> textLineHandler) {
            this.textLineHandler = textLineHandler;
        }

        public void setStatusHandler(BiConsumer<Boolean, String> connectionHandler) {
            this.connectionHandler = connectionHandler;
        }
    }

}
