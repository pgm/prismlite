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
<h1>Currently Open Requests</h1>

<g:if test="${records.size() == 0}">
    No active requests
</g:if>
<g:else>
<table>
    <tr>
        <th>RequestId</th>
        <th>Start</th>
        <th>URI</th>
    </tr>
    <g:each in="${records}" var="record">
        <tr>
            <td>${record.requestId}</td>
            <td>${new Date(record.start)}</td>
            <td>${record.request.uri}</td>
        </tr>
    </g:each>
</table>
</g:else>

</body>
</html>