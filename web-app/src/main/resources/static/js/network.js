function makeApiRequest() {
  const apiUrl = document.getElementById('apiUrl').value;
  const apiMethod = document.getElementById('apiMethod').value;
  const apiParams = document.getElementById('apiParams').value;
  const concurrencyCalls = document.getElementById('concurrencyCalls').value;
  const jsonPayload = document.getElementById('jsonPayload').value || null;
  const headers = document.getElementById('headers').value;
  const formTextFields = getElementValue('textData');
  const formUrlEncodedValues = getElementValue('formUrlEncodedValues');
  var password = ""

  if(apiUrl.length < 1)
  {
    alert("Please enter valid API URL");
    return;
  }

   if(!(concurrencyCalls >=1))
   {
    alert("Concurrency call range should be between 1 and 100");
    return;
   }
   if(!(concurrencyCalls >=1 && concurrencyCalls <= 100))
   {
    password = prompt("For Concurrent calls value above 100, password is mandatory. Please enter password.");
    if(password.length < 1)
    {
    alert("Password not provided!");
    return;
    }
   }

   const paramsJson = {};
   const paramsSplit = apiParams.split("\n")  || [];
   for(let i = 0; i< paramsSplit.length ; i++)
   {
    if(paramsSplit[i].trim().length > 0 && paramsSplit[i].indexOf(":") != -1)
    {
    const paramSplit = paramsSplit[i].split(":");
    paramsJson[paramSplit[0].trim()] = paramSplit.slice(1).join(':').trim().trim()
    }
   }


   var headerJson = {};
   if(headers.startsWith(":"))
   {
    headerJson = headers;
   }
   else
   {
   const headersSplit = headers.split("\n")  || [];
   for(let i = 0; i< headersSplit.length ; i++)
   {
    if(headersSplit[i].trim().length > 0 && headersSplit[i].indexOf(":") != -1)
    {
    const headerSplit = headersSplit[i].split(":");
    headerJson[headerSplit[0].trim()] = headerSplit.slice(1).join(':').trim().trim()
    }
   }
   }

 var proxyDetails = {}
 if(document.getElementById("proxyEnable").checked)
 {
   const userName = getElementValue('proxy_user_name');
   const password = getElementValue('proxy_password');
   const ip = getElementValue('proxy_ip') || "";
   const port = getElementValue('proxy_port');

   if(ip.length < 1)
   {
   alert("Please provide valid Proxy IP")
   return
   }

    proxyDetails = {
    "user_name" : userName,
    "password" : password,
    "ip" : ip,
    "port" : port
    }
 }

  const json = {
                   "url": apiUrl,
                   "method": apiMethod,
                   "headers": headerJson,
                   "concurrency_calls": concurrencyCalls,
                   "params": paramsJson,
                   "password" : password,
                   "proxy_meta" : proxyDetails
               }

const formData = new FormData();
formData.append("meta_json", JSON.stringify(json));

if(document.getElementById("bodyEnable").checked)
{
if(getElementValue("body") == "json")
{
formData.append("json_payload", jsonPayload);
}
else if(getElementValue("body") == "form")
{
   const formTextFieldsSplit = formTextFields.split("\n")  || [];
   for(let i = 0; i< formTextFieldsSplit.length ; i++)
   {
    if(formTextFieldsSplit[i].trim().length > 0)
    {
    const formTextFieldSplit = formTextFieldsSplit[i].split(":");
    formData.append(formTextFieldSplit[0].trim(), formTextFieldSplit.slice(1).join(':').trim());
    }
   }

var fileKeysSize = document.getElementById("fileData").children.length /3;
for(let i = 1; i<= fileKeysSize ; i++)
{
    const fileKey = getElementValue(`fileKey${i}`) || "";
    if(fileKey.length > 0)
    {
    const files = document.getElementById(`fileValue${i}`).files;
       for(let i = 0; i< files.length ; i++)
       {
        formData.append(fileKey, files[i]);
       }
    }
}
}
else if(getElementValue("body") == "formUrlEncoded")
{
   const formUrlEncodedJson = {};
   const formUrlEncodedValuesSplit = formUrlEncodedValues.split("\n")  || [];
   for(let i = 0; i< formUrlEncodedValuesSplit.length ; i++)
   {
    if(formUrlEncodedValuesSplit[i].trim().length > 0 && formUrlEncodedValuesSplit[i].indexOf(":") != -1)
    {
    const formUrlEncodedValueSplit = formUrlEncodedValuesSplit[i].split(":");
    formUrlEncodedJson[formUrlEncodedValueSplit[0].trim()] = formUrlEncodedValueSplit.slice(1).join(':').trim().trim()
    }
   }
   formData.append("form_urlencoded", JSON.stringify(formUrlEncodedJson));
}
}
  const requestOptions = {
    method: "POST",
    body: formData,
  };

  unHideElement("loading");
  fetch("/api/v1/concurrency", requestOptions)
    .then(response => response.json())
    .then(data => {
      if(handleRedirection(data))
      {
          hideElement("loading");
          return;
      }
      // Handle new standardized response format
      var responseData = data;
      if (data.success !== undefined) {
          // New format - use data field if available, otherwise use the whole response
          responseData = data.data || data;
      }
      document.getElementById('output').value = JSON.stringify(responseData, null, 2);
      hideElement("loading");
    })
    .catch(error => {
      hideElement("loading");
      document.getElementById('output').value = 'Error: ' + error.message;
    });
}

function changeBody()
{
   if(getElementValue("body") == "form")
   {
    hideElement("jsonPayload");
    hideElement("jsonLabel");
    hideElement("formUrlEncodedData");
    hideElement("formUrlEncodedDataLabel");

    unHideElement("formData");
    unHideElement("formLabel");
   }
   else if(getElementValue("body") == "formUrlEncoded")
   {
    hideElement("jsonPayload");
    hideElement("jsonLabel");
    hideElement("formData");
    hideElement("formLabel");

    unHideElement("formUrlEncodedData");
    unHideElement("formUrlEncodedDataLabel");
   }
   else
   {
    hideElement("formUrlEncodedData");
    hideElement("formUrlEncodedDataLabel");
    hideElement("formData");
    hideElement("formLabel");

    unHideElement("jsonPayload");
    unHideElement("jsonLabel");

   }
}

function enableBody()
{
if(document.getElementById("bodyEnable").checked)
{
unHideElement("showBody");
}
else
{
hideElement("showBody")
}
}

function enableProxy()
{
if(document.getElementById("proxyEnable").checked)
{
unHideElement("showProxy");
}
else
{
hideElement("showProxy")
}
}


function changeMethod()
{
if(getElementValue("apiMethod") == "GET")
{
document.getElementById("bodyEnable").checked = false;
enableBody();
document.getElementById("bodyEnable").disabled = true;
}
else
{
document.getElementById("bodyEnable").disabled = false;
}
}


function copyAll(){
if(getElementValue("output").length < 1)
{
alert("No response found");
return;
}
copyToClipboard(document.getElementById("output").value);
}

function downloadResponse()
{
if(getElementValue("output").length < 1)
{
alert("No response found");
return;
}
const data = {
"text" : getElementValue("output")
}

      fetch( "/api/v1/download/text", {
           method: "POST",
            headers: {
                'Content-Type' : 'application/json'
            },
            body: JSON.stringify(data)
      })
      .then(response =>
      {
      return response.text();
      }
      ).then(data=> {
const res = JSON.parse(data)
if(handleRedirection(res))
{
   return;
}
// Handle new standardized response format
var responseData = res;
if (res.success !== undefined) {
    responseData = res.data || res;
}
window.open(`/api/v1/download/text?file_name=${responseData.file_name}`, "_self")
}).catch(error => {
   console.log("Something went wrong. Server might be down");
});
}

function addFile()
{
var index = (document.getElementById("fileData").children.length /3) + 1;
var input = document.createElement("input");
input.type = "text";
input.id = `fileKey${index}`;
input.style="width:100px";
input.placeholder = "Key Name";
document.getElementById("fileData").append(input);

input = document.createElement("input");
input.type = "file";
input.id = `fileValue${index}`;
input.style="width:250px";
input.setAttribute('multiple','');
document.getElementById("fileData").append(input);
document.getElementById("fileData").append(document.createElement("br"));
}


function addFile()
{
var index = (document.getElementById("fileData").children.length /3) + 1;
var input = document.createElement("input");
input.type = "text";
input.id = `fileKey${index}`;
input.style="width:100px";
input.placeholder = "Key Name";
document.getElementById("fileData").append(input);

input = document.createElement("input");
input.type = "file";
input.id = `fileValue${index}`;
input.style="width:250px";
input.setAttribute('multiple','');
document.getElementById("fileData").append(input);

document.getElementById("fileData").append(document.createElement("br"));

}


function removeFile()
{
var childLength = document.getElementById("fileData").children.length;
if(childLength == 3)
{
return;
}
document.getElementById("fileData").children[childLength-1].remove();
var index = childLength/3;
document.getElementById(`fileValue${index}`).remove();
document.getElementById(`fileKey${index}`).remove();
}
