[#ftl]
<h2>Agents</h2>

<table id="agents">
  <tr>
    <td class="agentName"></td>
    <td class="agentURI"></td>
  </tr>
</table>

<script type="text/javascript">
  var agentData = {
    agents:[]
  };
 
  var agentDirective = {
    'tr':{
      'agent<-agents':{
        'td.agentName':'agent.name',
        'td.agentURI':'agent.agentURI'
      }
    }
  };
  
  $.ajax({
    url: "${path}",
    success: function(data){
      agentData = {
        agents: data
      };
      
      $('table#agents').render(agentData, agentDirective); 
    }
  });

</script>
