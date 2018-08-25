function scanPage()
{
    scanFrame(document);

    var allUrlsList = [];

    var a = document.getElementsByTagName('video');
    for (var i = 0; i < a.length; i++)
    {
        var link = a[i];
        var u = false;
        if (link.src)
            u = link.src;
        if (!u && link.hasAttribute('data-thumb'))
        {
            u = myTrim(link.getAttribute('data-thumb'));
            if (u.indexOf("http") == -1)
                u = "http:" + u;
        }
        if ( u)
        {
            var title = '';
            if (link.hasAttribute('alt'))
                title = myTrim(link.getAttribute('alt'));
            else if (link.hasAttribute('title'))
                title = myTrim(link.getAttribute('title'));
            if (!title)
                title=document.title;
            var cl = "";
            if (link.hasAttribute('class'))
                cl = myTrim(link.getAttribute('class'));

            allUrlsList.push({'url': u,'title': title});
        }
    }

    console.log(JSON.stringify(allUrlsList));
    if (allUrlsList.length > 0)
        window.bridge.loadVideo(allUrlsList[0].url);
}

var observer = new window.MutationObserver(function (mutations) {
    scanPage();
});

observer.observe(document, {
    childList: true, // listen to changes to node children
    subtree: true // listen to changes to descendants as well
});

setTimeout(scanPage, 1000);

function scanFrame(document) {
    var iframe = document.getElementsByTagName('iframe');
    for (var i = 0; i < iframe.length; i++) {
        var src = iframe[i].getAttribute("src");
        console.log("iframe src=" + src);
        if (window.now) {
            if (src.indexOf(now) != -1)
                window.bridge.loadUrl(src);
            else
                scanFrame(iframe[i].contentWindow.document);
        } else {
            window.bridge.loadUrl(src);
        }
    }
}