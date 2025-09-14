var loginInProcess = false;
    function login() {
    if(loginInProcess)
    {
        return
    }
    loginInProcess = true;
    const name = document.getElementById('name').value;
    const password = document.getElementById('password').value;

     unHideElement("loading");

        var payload = {
        "name" : name,
        "password" : password
        }
          fetch( "/api/v1/authenticate", {
               method: "POST",
                headers: {
                    'Content-Type' : 'application/json'
                },
                body: JSON.stringify(payload)
          })
          .then(response =>
          {
          return response.text();
          }
          ).then(data=> {
            hideElement("loading");
            const res = JSON.parse(data);
            // Handle new standardized response format
            if (!res.success) {
                 loginInProcess = false;
                alert(res.message || "Invalid credentials");
                return;
            }
            if(new URLSearchParams(window.location.search).get('post'))
            {
                window.close();
            }
            const redirectURI =  new URLSearchParams(window.location.search).get('redirect_uri') || '/';
            window.open (redirectURI, "_self");
          }).catch(error => {
             loginInProcess = false;
             console.log(error)
             hideElement("loading");
             alert("Something went wrong. Server might be down");
          });
    }

document.addEventListener("keydown", function(event) {
  if (event.key === "Enter") {
    login()
  }
});

//document.getElementById('name').value = "test";
//document.getElementById('password').value = "test@123";
//setTimeout(function() {
//    if(document.getElementById('name').value == "test")
//    {
//        toastMessage('Auto-login for test account')
//        setTimeout(login, 2000);
//    }
//}, 3000);


function toastMessage(message) {
    var x = document.getElementById("snackbar");
    x.innerHTML = message;
    x.className = "show";
    setTimeout(function() {
        x.className = x.className.replace("show", "");
    }, 3000);
}