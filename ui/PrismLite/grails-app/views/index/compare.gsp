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
<h1>Comparing</h1>

<g:link controller="index" action="show" id="${leftId}">Left: ${leftId}</g:link>
<g:link controller="index" action="show" id="${rightId}">Right: ${rightId}</g:link>

<h1>Diff</h1>
<pre>
${diff.encodeAsHTML()}
</pre>

</body>
</html>