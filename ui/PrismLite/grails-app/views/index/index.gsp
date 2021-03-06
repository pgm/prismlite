<!DOCTYPE html>
<html>
	<head>
		<meta name="layout" content="main"/>
		<title>Welcome to Grails</title>
		<style type="text/css" media="screen">
			#status {
				background-color: #eee;
				border: .2em solid #fff;
				margin: 2em 2em 1em;
				padding: 1em;
				width: 12em;
				float: left;
				-moz-box-shadow: 0px 0px 1.25em #ccc;
				-webkit-box-shadow: 0px 0px 1.25em #ccc;
				box-shadow: 0px 0px 1.25em #ccc;
				-moz-border-radius: 0.6em;
				-webkit-border-radius: 0.6em;
				border-radius: 0.6em;
			}

			.ie6 #status {
				display: inline; /* float double margin fix http://www.positioniseverything.net/explorer/doubled-margin.html */
			}

			#status ul {
				font-size: 0.9em;
				list-style-type: none;
				margin-bottom: 0.6em;
				padding: 0;
			}

			#status li {
				line-height: 1.3;
			}

			#status h1 {
				text-transform: uppercase;
				font-size: 1.1em;
				margin: 0 0 0.3em;
			}

			#page-body {
				margin: 2em 1em 1.25em 18em;
			}

			h2 {
				margin-top: 1em;
				margin-bottom: 0.3em;
				font-size: 1em;
			}

			p {
				line-height: 1.5;
				margin: 0.25em 0;
			}

			#controller-list ul {
				list-style-position: inside;
			}

			#controller-list li {
				line-height: 1.3;
				list-style-position: inside;
				margin: 0.25em 0;
			}

			@media screen and (max-width: 480px) {
				#status {
					display: none;
				}

				#page-body {
					margin: 0 1em 1em;
				}

				#page-body h1 {
					margin-top: 0;
				}
			}
		</style>
	</head>
	<body>

    <g:link controller="index" action="activeRequests">Show active requests</g:link>

    <g:form method="GET">
        <g:submitButton name="submit" value="Search by URI RegExp"/><g:textField name="uriPattern" style="width: 40em;" value="${uriPatternStr}"/>
    </g:form>

    <g:if test="${prevPageSize > 0}">
        <g:link action="index" params="${[count: count, start: prevFirst]}">Previous ${prevPageSize}</g:link>
    </g:if>

    <g:if test="${nextPageSize > 0}">
        <g:link action="index" params="${[count: count, start: last-1]}">Next ${nextPageSize}</g:link>
    </g:if>

    <g:form method="GET">
        <table>

        <tr>
            <th>Id</th><th>Timestamp</th><th>Status</th><th>Method</th><th>URI</th><th>RespHash</th><th colspan="2"><g:actionSubmit action="compare" value="Compare"/></th>
        </tr>
        <g:each in="${records}" var="record">
            <tr>
                <td>
                    <g:link controller="index" action="show" id="${record.id}">${record.id}</g:link>
                </td>
                <td><g:formatDate date="${new Date(record.recording.start)}" type="datetime"/></td>
                <td>${record.recording.response.status}</td><td>${record.recording.request.method}</td><td>${record.recording.request.uri}</td>
                <td>${prismlite.IndexController.getResponseHash(record.recording).substring(0,6)}</td>
                <td><input type="radio" name="left" value="${record.id}"></td><td><input type="radio" name="right" value="${record.id}"></td>
            </tr>
        </g:each>
    </g:form>
    </table>
	</body>
</html>
