var appWindow;
var origin;

window.addEventListener("message", function (e) {
    switch (e.data.type) {
        case "init":
            if (appWindow == null) {
                appWindow = e.source;
                origin = e.origin;
            }
            break;
        case "callback":
        {
            org.vaadin.demo.hw.serialCallBack(e.data.payload);
            break;
        }
    }
});

function disconnect()
{
    appWindow.postMessage({"type":"disconnect"}, origin);
}


function connect()
{
    appWindow.postMessage({"type":"connect"}, origin);
}

function sendText(s)
{
    appWindow.postMessage({"type":"writeSerial","text": s}, origin);
}


