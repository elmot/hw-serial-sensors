var appWindow;
var origin;

window.addEventListener("message", function (e) {
    if (e.data.type == "init") {
        if (appWindow == null) {
            appWindow = e.source;
            origin = e.origin;
        }
    } else org.vaadin.demo.hw.serialCallBack(e.data);
});

function disconnect() {
    appWindow.postMessage({"type": "disconnect"}, origin);
}


function connect() {
    appWindow.postMessage({"type": "connect"}, origin);
}

function sendText(s) {
    appWindow.postMessage({"type": "writeSerial", "text": s}, origin);
}


