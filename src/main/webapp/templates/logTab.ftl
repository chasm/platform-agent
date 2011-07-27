[#ftl]
<table id="logs"></table>
<div id="logPager"></div>
<script type="text/javascript">
  jQuery("#logs").jqGrid({
    url: "${path}",
    datatype: "json",
    rowNum: 10,
    rowList: [10,20,30],
    pager: '#logPager',
    sortname: 'agentURI',
    viewrecords: true,
    sortorder: "desc",
    caption: "Log Items"
  });
  jQuery("#logs").jqGrid('navGrid','#logPager',{ edit:false, add:false, del:false });
</script>
