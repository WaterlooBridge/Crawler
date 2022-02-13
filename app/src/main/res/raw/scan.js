<script type="text/javascript">
window.bridge.scanPage = function() {
    var allUrlsList = [];

    var a = document.getElementsByTagName('video');
    for (var i = 0; i < a.length; i++) {
        var link = a[i];
        var u = false;
        if (link.src)
            u = link.src;
        else if (link.hasAttribute('src'))
            u = link.getAttribute('src');
        if (u && u.startsWith('http'))
            allUrlsList.push({'url': u});
    }

    console.log(JSON.stringify(allUrlsList));
    if (allUrlsList.length > 0)
        window.bridge.loadVideo(allUrlsList[0].url);
}

setInterval(window.bridge.scanPage, 1000);
</script>
