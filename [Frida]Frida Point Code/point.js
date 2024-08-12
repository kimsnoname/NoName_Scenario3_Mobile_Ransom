Java.perform(function() {
    let WebViewActivity = Java.use("com.example.nonameapp.WebViewActivity$WebAppInterface");
    WebViewActivity["encrypt"].implementation = function (str, str2) {
        console.log(`WebViewActivity.encrypt is called: str=${str}, str2=${str2}`);
        str = "{\"point\": 5555, \"Dummy1\": \"AAAAAAAAAAAAAAAAAAAA\", \"Dummy2\": \"BBBBBBBBBBBBBBBBBBBB\", \"Dummy3\": \"CCCCCCCCCCCCCCCCCCCC\"}";
        console.log(`WebViewActivity.encrypt is changed: str=${str}, str2=${str2}`);
        let result = this["encrypt"](str, str2);
        console.log(`WebViewActivity.encrypt result=${result}`);
        return result;
    };
});