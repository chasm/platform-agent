[#ftl]
<div id="innerTabs">
  <ul>
		<li><a href="#innerTabs-1">Monitor</a></li>
		<li><a href="#innerTabs-2">Manage</a></li>
		<li><a href="#innerTabs-3">Connect</a></li>
  </ul>
  <div id="innerTabs-1">Loading...</div>
  <div id="innerTabs-2">Loading...</div>
  <div id="innerTabs-3">Loading...</div>
</div>
<script type="text/javascript">
  $('#innerTabs').tabs();
  $('#innerTabs').tabs('url', 0, "/logs/${uuid}");
  $('#innerTabs').tabs('url', 1, "/agents/${uuid}");
  $('#innerTabs').tabs('url', 2, "/cnxns/${uuid}");
  $('#innerTabs').tabs('load', 0);
</script>
