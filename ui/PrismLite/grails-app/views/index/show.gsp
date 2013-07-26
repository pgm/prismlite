<%--
  Created by IntelliJ IDEA.
  User: pmontgom
  Date: 7/24/13
  Time: 10:11 PM
  To change this template use File | Settings | File Templates.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="main"/>
  <title></title>
    <r:require module="jquery"/>
</head>
<body>
<h1>Request</h1>
<table>
    <tr><td>URI:</td><td>${record.request.uri}</td></tr>
    <tr><td>Method</td><td>${record.request.method}</td></tr>
    <tr><td>Content</td><td>${record.request.request}</td></tr>
    <tr><td>Start</td><td>${new Date(record.start)}</td></tr>
    <tr><td>Duration</td><td>${record.stop-record.start}</td></tr>
</table>
<h2>Headers</h2>
<table>
    <g:each in="${record.request.headersList}" var="header">
        <tr><td>${header.name}</td><td>${header.value}</td></tr>
    </g:each>
</table>

<h1>Response</h1>
<table>
    <tr><td>status</td><td>${record.response.status}</td></tr>
    <tr><td>content type</td><td>${record.response.contentType}</td></tr>
</table>
<h2>Headers</h2>
<table>
    <tr><td colspan="2">Headers</td></tr>
    <g:each in="${record.response.headersList}" var="header">
        <tr><td>${header.name}</td><td>${header.value}</td></tr>
    </g:each>
</table>
<h2>Content</h2>
<input type="button" value="Reformat JSON" id="reformat-json-button">
<pre id="response-content">
${new String(record.response.content.toByteArray()).encodeAsHTML()}
</pre>
<r:script>
    $("#reformat-json-button").on("click", function() {
        var node = $("#response-content")[0];
        var content = node.innerText;
        console.log(content);
        var obj = JSON.parse(content);
        node.innerText = JSON.stringify(obj, null, 2);
    })  ;
</r:script>
</body>
</html>