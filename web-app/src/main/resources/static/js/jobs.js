    var otpReference;

   async function addJob(otp) {
    const date = document.getElementById('date').value.replace('T', ' ');
    const isRecurring = document.getElementById("is_recurring").checked;
    const dayInterval = isRecurring ? document.getElementById('day_interval').value : -1;
    if(date.length == 0)
    {
        alert("Please enter valid Execution Date & Time");
        return;
    }
    if(isRecurring)
    {
    if(dayInterval.length == 0)
    {
        alert("Please enter either valid Execution Date & Time");
        return;
    }
    }
   
    var data;
     if(document.getElementById('task').value == "mail")
     {

         subject = document.getElementById('subject').value;
         fromAddress = document.getElementById('fromAddress').value;
         toAddress = document.getElementById('toAddress').value;
         emailMessage = document.getElementById('emailMessage').value;

           const email = {
           "to" : toAddress,
           "message" : emailMessage,
           "subject" : subject,
           "from_address" : fromAddress,
           "to_address" : toAddress
           }

          data = JSON.stringify(email)

     }
    if(document.getElementById('task').value == "mail" && otp == null)
    {
    if(emailMessage.length < 1)
    {
        alert("Please enter valid message");
        return;
    }

    if(subject.length < 1)
    {
        alert("Please enter valid subject");
        return;
    }
    if(! /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(fromAddress))
    {
        alert("Invalid from address");
        return;
    }

    if(! /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(toAddress))
    {
        alert("Invalid to address");
        return;
    }

   unHideElement("loading");
   otpReference = await initiateOTP(fromAddress)
   hideElement("loading");

   openPrompt()
   return

    }

        var payload = {
        "task" : document.getElementById('task').value,
        "data" : data,
        "execution_date_time" : date,
        "day_interval" : dayInterval,
        "is_recurring" : isRecurring,
        "otp_reference" : otpReference,
        "otp" : otp
        }
        unHideElement("loading");
          fetch( "/api/v1/jobs", {
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
            const res = JSON.parse(data);
            // Handle new standardized response format
            if (res.success) {
                alert(res.message);
                if(res.message.includes('Job has been scheduled successfully'))
                    resetInput();
            } else {
                alert(res.message || "An error occurred");
            }
          }).catch(error => {
             alert("Something went wrong. Server might be down");
          });

        hideElement("loading");
    }


  function initiateOTP(email) {
    return new Promise(resolve => {
       fetch( "/api/v1/initiate/otp?email=" + email, {
            method: "POST"
       })
       .then(response =>
       {
       return response.text();
       }
       ).then(data=> {
        var res = JSON.parse(data)
        // Handle new standardized response format
        if (res.success) {
            resolve(res.data.otp_reference);
        } else {
            alert(res.message || "Failed to initiate OTP");
            resolve(null);
        }
       }).catch(error => {
           console.log(error)
           alert("Something went wrong. Server might be down");
           resolve(null);
       });
    });

 }
        function getJobs() {
              fetch( "/api/v1/jobs/list", {
                   method: "GET"
              })
              .then(response =>
              {
              return response.text();
              }
              ).then(data=> {
               var res = JSON.parse(data)
                // Handle new standardized response format
                if (res.success) {
                    var jobListOptions = ""
                    var jobData = res.data || res; // fallback to old format
                    for (var job in jobData) {
                        jobListOptions += "<option value=" + job + ">" + jobData[job] + "</option>";
                    }
                    document.getElementById("task").innerHTML = jobListOptions;
                    handleFields()
                } else {
                    alert(res.message || "Failed to load jobs");
                }
              }).catch(error => {
                  console.log(error)
                 alert("Something went wrong. Server might be down");
              });
        }

        getJobs()

function handleFields()
{
    if(document.getElementById('task').value == "mail")
    {
        document.getElementById('emailTask').style.display = "block"
    }
    else
    {
        document.getElementById('emailTask').style.display = "none"
    }
}

function getFormattedDateVal(value)
{
return value < 10 ? '0' + value : value;
}


function showOrHideRecurring()
{
    if(document.getElementById("recurring_info").style.display == "block")
    {
        document.getElementById("recurring_info").style.display = "none"
    }
    else
    {
        document.getElementById("recurring_info").style.display = "block"
    }
}


        function openPrompt() {
            setElementValue('otp_value','')
            document.getElementById('promptOverlay').style.display = 'flex';
        }

        function closePrompt() {
            document.getElementById('promptOverlay').style.display = 'none';
        }

function submitPrompt() {
closePrompt();
var otp = getElementValue('otp_value');
   if(otp == '' || isNaN(otp))
   {
    alert("Invalid OTP")
    return
    }
   addJob(otp)
}


var dayIntervalOptions = "";
for (var i=1 ;i< 367; i++)  {
    dayIntervalOptions += "<option >" + i + "</option>";
}

function resetInput()
{
setElementValue('subject','')
setElementValue('fromAddress', '')
setElementValue('toAddress', '')
setElementValue('emailMessage', '')
}

document.getElementById("day_interval").innerHTML = dayIntervalOptions;

const defaultScheduleDate = new Date(new Date().getTime() + (5*60000));

document.getElementById("timestampDisplay").textContent = defaultScheduleDate.getTime();

document.getElementById('date').value = `${defaultScheduleDate.getFullYear()}-${getFormattedDateVal(defaultScheduleDate.getMonth() + 1)}-${getFormattedDateVal(defaultScheduleDate.getDate())}T${getFormattedDateVal(defaultScheduleDate.getHours())}:${getFormattedDateVal(defaultScheduleDate.getMinutes())}`


document.getElementById("date").addEventListener("input", function () {
    const input = this.value;
    if (!input) {
        return;
    }

    const date = new Date(input);
    const epochMillis = date.getTime();
    document.getElementById("timestampDisplay").textContent = epochMillis;
});