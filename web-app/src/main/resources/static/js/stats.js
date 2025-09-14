var isStatsFileReady = true
function loadStats() {
const statsIdInput = document.getElementById('statsId');

    if (statsIdInput.value == null || statsIdInput.value.length < 1) {
        alert('Please enter a valid Request Id');
        return;
    }

var stats_id = statsIdInput.value;
    unHideElement("loading")
    fetch('/api/v1/csv/parse?stats_id=' + stats_id, {
        method: 'POST'
    })
    .then(response => {
        return response.json();
    })
    .then(data => {
    hideElement("loading");
     if(handleRedirection(data))
     {
         return;
     }
     // Handle new standardized response format
     if (!data.success) {
        alert(data.message || data.error || "An error occurred");
        return;
     }
     // Use data from the new format, fallback to old format
     var responseData = data.data || data;
     document.getElementById("output").innerHTML = responseData.table_data;
     document.getElementById("total").innerHTML = "TOTAL : " + responseData.total +  "<br/><br/> STATUS : " + (!isStatsFileReady? 'RUNNING' : 'COMPLETED');
     isStatsFileReady = responseData.is_completed

    })
    .catch(error => {
        hideElement("loading");
        alert('Something went wrong. Please check the console for error details.')
        console.error('There was a problem with the fetch operation:', error);
    });
}

function initiateStats()
{

    if(document.getElementById('configuration').style.display == 'none')
        convertFormDataToXml()

    if (document.getElementById('requestData').files[0] == undefined || document.getElementById('requestData').files[0].length == 0) {
        if(getElementValue("requestDataText") == undefined || getElementValue("requestDataText").length == 0){
          alert('Please upload request data file or enter request data text');
          return;
          }
    }

        if (getElementValue('configuration_area') == undefined) {
            alert('Please enter valid xml configuration');
            return;
        }

const formData = new FormData();
formData.append("configuration", getElementValue('configuration_area'));
formData.append("request_data", document.getElementById('requestData').files[0]);
formData.append("request_data_text", getElementValue("requestDataText"));


  const requestOptions = {
    method: "POST",
    body: formData,
  };

  unHideElement("loading");
  fetch("/api/v1/stats", requestOptions)
    .then(response => response.json())
    .then(data => {
      if(handleRedirection(data))
      {
          hideElement("loading");
          return;
      }
       hideElement("loading");
       // Handle new standardized response format
       if (!data.success) {
          alert(data.message || data.error || "An error occurred");
          return;
       }
      alert(data.message);
      // Use data from the new format, fallback to old format
      var responseData = data.data || data;
      setElementValue('statsId', responseData.request_id)
      loadStats()
    })
    .catch(error => {
     console.log(error)
      hideElement("loading");
    });
}

function downloadRawResponse()
{
const statsIdInput = document.getElementById('statsId');
    if (statsIdInput.value == null || statsIdInput.value.length < 1) {
        alert('Please enter a valid Request Id');
        return;
    }

    window.open("/uploads/RawResponse_" + statsIdInput.value + ".txt", "_self")
}

function downloadCSVResponse()
{
    const statsIdInput = document.getElementById('statsId');

    if (statsIdInput.value == null || statsIdInput.value.length < 1) {
        alert('Please enter a valid Request Id');
        return;
    }

    if(isStatsFileReady != true)
    {
      alert('Stats not completed yet')
      return
    }

    window.open("/uploads/" + statsIdInput.value + ".csv", "_self")
}

function parseChromeHeaders(chromeHeaders) {
    let parsedHeaders = '';
    const lines = chromeHeaders.split('\n').map(line => line.trim()).filter(line => line.length > 0);
    for (let i = 0; i < lines.length; i += 2) {
        let key = lines[i];
        if (key.startsWith(':')) {
            key = key.replace(/^:/, '');
        }
        if (key.endsWith(':')) {
            key = key.replace(/:$/, '');
        }

        parsedHeaders = `${parsedHeaders}${key}: ${lines[i + 1] ? lines[i + 1].trim() : ''}\n`;
    }
    return parsedHeaders;
}

function buildXmlSectionFromColonSeparated(input, isForResponse = false) {
    let xmlSection = '';
    if (input && input.trim().length > 0) {
       if(input.startsWith(':'))
        {
          input = parseChromeHeaders(input);
          console.log("Parsed Headers: ", input);
        }
        xmlSection += '\n';
        input.split(/\r?\n/).forEach(line => {
            const [key, ...rest] = line.split(':');

            if (key && rest.length > 0 && (key.indexOf('Content-Length') == -1 && key.indexOf('Content-Type') == -1))
            {
                const value = rest.join(':');
                if(isForResponse)
                {
                    const isPlaceholder = key.trim().startsWith('{') && key.trim().endsWith('}') ? 'true' : 'false';
                    const modifiedKey = key.trim().replace(/^\{|\}$/g, '');
                    xmlSection += `    <header name="${modifiedKey}" placeholder="${isPlaceholder}">${value.trim()}</header>`;
                }
                else
                {
                    xmlSection += `    <${key.trim()}>${value.trim()}</${key.trim()}>`;
                }
                xmlSection += '\n';
            }
        });

        xmlSection += '\n';

    }
    if(isForResponse && !(xmlSection.trim().length && xmlSection.trim().length > 1))
    {
        alert("Please enter at least one response header");
        throw new Error("No response headers found");
    }
    return xmlSection;
}

function convertFormDataToXml() {
    const isTest = document.getElementById('isTest').checked;
    const skipFirstRequestDataRow = document.getElementById('skipFirstRequestDataRow').checked;
    const disableParallelCalls = document.getElementById('disableParallelCalls').checked;
    const url = document.getElementById('url').value;
    const method = document.getElementById('method').value;
    const headers = document.getElementById('headers').value;
    const params = document.getElementById('params').value;
    const jsonBody = document.getElementById('jsonBody').value;
    const requestBatchSize = document.getElementById('requestBatchSize').value;
    const requestBatchInterval = document.getElementById('requestBatchInterval').value;
    const response = document.getElementById('response').value;
    const placeholderHandler = document.getElementById('placeholderHandler').value;

    // Construct XML string
    let xml = `<?xml version="1.0"?>\n`;
    xml += `<configuration xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"stats-meta.xsd\">\n`;
    xml += `  <is-test>${isTest}</is-test>\n`;
    xml += `  <skip-first-request-data-row>${skipFirstRequestDataRow}</skip-first-request-data-row>\n`;
    xml += `  <disable-parallel-calls>${disableParallelCalls}</disable-parallel-calls>\n`;
    xml += `  <url>${url}</url>\n`;
    xml += `  <method>${method}</method>\n`;
    xml += `  <headers>${buildXmlSectionFromColonSeparated(headers)}  </headers>\n`;
    xml += `  <raw-request-headers></raw-request-headers>\n`;
    xml += `  <params>${buildXmlSectionFromColonSeparated(params)}  </params>\n`;
    xml += `  <json-body>${jsonBody}</json-body>\n`;
    xml += `  <request-batch-size>${requestBatchSize}</request-batch-size>\n`;
    xml += `  <request-batch-interval>${requestBatchInterval}</request-batch-interval>\n`;
    xml += `  <response>${buildXmlSectionFromColonSeparated(response, true)}  </response>\n`;
    xml += `  <placeholder-handler>${placeholderHandler}</placeholder-handler>\n`;
    xml += `</configuration>`;

    // Set XML to configuration_area
    document.getElementById('configuration_area').value = xml;
    // Call the existing submit function
}
