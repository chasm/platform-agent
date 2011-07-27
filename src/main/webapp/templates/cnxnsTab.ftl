[#ftl]
<h2>Connections</h2>

<table id="cnxns">
  <tr>
    <td class="cnxnName"></td>
    <td class="agentURI"></td>
  </tr>
</table>

<script type="text/javascript">
  var cnxnData = {
    cnxns:[
      {
        "agentURI":"255.0.0.0/pathological",
        "name":"Jim Bob"
      },
      {
        "agentURI":"0.255.0.0/psychopath",
        "name":"Sue Ellen"
      },
      {
        "agentURI":"0.0.255.0/neuropathy",
        "name":"Patty Jo"
      }
    ]
  };
 
  //declaration of the actions PURE has to do
  var cnxnDirective = {
    'tr':{
      'cnxn<-cnxns':{
        'td.cnxnName':'cnxn.name',
        'td.agentURI':'cnxn.agentURI'
      }
    }
  };

  // note the use of render instead of autoRender
  $('table#cnxns').render(cnxnData, cnxnDirective);
</script>
