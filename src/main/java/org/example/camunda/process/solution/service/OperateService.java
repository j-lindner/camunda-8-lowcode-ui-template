package org.example.camunda.process.solution.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.CamundaOperateClientConfiguration;
import io.camunda.operate.auth.Authentication;
import io.camunda.operate.auth.JwtAuthentication;
import io.camunda.operate.auth.JwtCredential;
import io.camunda.operate.auth.TokenResponseMapper;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.model.FlowNodeInstance;
import io.camunda.operate.model.ProcessDefinition;
import io.camunda.operate.model.ProcessInstance;
import io.camunda.operate.model.ProcessInstanceState;
import io.camunda.operate.model.SearchResult;
import io.camunda.operate.model.Variable;
import io.camunda.operate.search.FlowNodeInstanceFilter;
import io.camunda.operate.search.ProcessDefinitionFilter;
import io.camunda.operate.search.ProcessInstanceFilter;
import io.camunda.operate.search.SearchQuery;
import io.camunda.operate.search.Sort;
import io.camunda.operate.search.SortOrder;
import io.camunda.operate.search.VariableFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.example.camunda.process.solution.utils.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;

@Service
@EnableCaching
public class OperateService {

  private static final Logger LOG = LoggerFactory.getLogger(OperateService.class);

  @Value("${camunda.client.auth.client-id:notProvided}")
  private String clientId;

  @Value("${camunda.client.auth.client-secret:notProvided}")
  private String clientSecret;

  @Value("${camunda.client.cluster-id:notProvided}")
  private String clusterId;

  @Value("${camunda.client.region:notProvided}")
  private String region;

  @Value("${identity.clientId:notProvided}")
  private String identityClientId;

  @Value("${identity.clientSecret:notProvided}")
  private String identityClientSecret;

  @Value("${operateUrl:notProvided}")
  private String operateUrl;

  @Value("${keycloakUrl:notProvided}")
  private String keycloakUrl;

  private CamundaOperateClient client;

  private CamundaOperateClient getCamundaOperateClient() throws OperateException {
    if (client == null) {
      String targetOperateUrl = operateUrl;
      Authentication auth = null;
      if (!"notProvided".equals(clientId)) {
        try {
          URL operateUrl =
              URI.create("https://" + region + ".operate.camunda.io/" + clusterId).toURL();
          URL authUrl = URI.create("https://login.cloud.camunda.io/oauth/token").toURL();

          JwtCredential credentials =
              new JwtCredential(clientId, clientSecret, "operate.camunda.io", authUrl, null);
          ObjectMapper objectMapper = new ObjectMapper();
          TokenResponseMapper tokenResponseMapper =
              new TokenResponseMapper.JacksonTokenResponseMapper(objectMapper);
          JwtAuthentication authentication =
              new JwtAuthentication(credentials, tokenResponseMapper);
          CamundaOperateClientConfiguration configuration =
              new CamundaOperateClientConfiguration(
                  authentication, operateUrl, objectMapper, HttpClients.createDefault());
          client = new CamundaOperateClient(configuration);
        } catch (MalformedURLException e) {
          throw new OperateException("Error with Operate URLs", e);
        }
      } else {
        try {
          String tokenUrl = keycloakUrl;

          URL operateUrl = URI.create(targetOperateUrl).toURL();
          URL authUrl = URI.create(tokenUrl).toURL();

          JwtCredential credentials =
              new JwtCredential(
                  identityClientId, identityClientSecret, identityClientId, authUrl, null);
          ObjectMapper objectMapper = new ObjectMapper();
          TokenResponseMapper tokenResponseMapper =
              new TokenResponseMapper.JacksonTokenResponseMapper(objectMapper);
          JwtAuthentication authentication =
              new JwtAuthentication(credentials, tokenResponseMapper);
          CamundaOperateClientConfiguration configuration =
              new CamundaOperateClientConfiguration(
                  authentication, operateUrl, objectMapper, HttpClients.createDefault());
          client = new CamundaOperateClient(configuration);
        } catch (MalformedURLException e) {
          throw new OperateException("Error with Operate URLs", e);
        }
      }
    }
    return client;
  }

  public ProcessDefinition getLatestProcessDefinition(String bpmnProcessId)
      throws OperateException {
    ProcessDefinitionFilter processDefinitionFilter =
        ProcessDefinitionFilter.builder().bpmnProcessId(bpmnProcessId).build();
    SearchQuery procDefQuery =
        new SearchQuery.Builder()
            .filter(processDefinitionFilter)
            .size(1)
            .sort(new Sort("version", SortOrder.DESC))
            .build();

    List<ProcessDefinition> def = getCamundaOperateClient().searchProcessDefinitions(procDefQuery);
    if (def != null && def.size() > 0) {
      return def.get(0);
    }
    return null;
  }

  public List<ProcessDefinition> getProcessDefinitions() throws OperateException {
    ProcessDefinitionFilter processDefinitionFilter = ProcessDefinitionFilter.builder().build();
    SearchQuery procDefQuery =
        new SearchQuery.Builder()
            .filter(processDefinitionFilter)
            .size(1000)
            .sort(new Sort("version", SortOrder.DESC))
            .build();

    return getCamundaOperateClient().searchProcessDefinitions(procDefQuery);
  }

  @Cacheable("processXmls")
  public String getProcessDefinitionXmlByKey(Long key) throws OperateException {
    LOG.info("Entering getProcessDefinitionXmlByKey for key " + key);
    return getCamundaOperateClient().getProcessDefinitionXml(key);
  }

  public Map<String, Set<JsonNode>> listVariables() throws OperateException, IOException {
    List<Variable> vars =
        getCamundaOperateClient()
            .searchVariables(
                new SearchQuery.Builder().filter(new VariableFilter()).size(1000).build());
    Map<String, Set<JsonNode>> result = new HashMap<>();
    for (Variable var : vars) {
      if (!result.containsKey(var.getName())) {
        result.put(var.getName(), new HashSet<>());
      }
      result.get(var.getName()).add(JsonUtils.toJsonNode(var.getValue()));
    }
    return result;
  }

  public SearchResult<ProcessInstance> getProcessInstances(
      String bpmnProcessId, ProcessInstanceState state, Integer pageSize, Long after)
      throws OperateException {
    SearchQuery q =
        new SearchQuery.Builder()
            .filter(
                ProcessInstanceFilter.builder().state(state).bpmnProcessId(bpmnProcessId).build())
            .size(pageSize)
            .build();
    if (after != null) {
      q.setSearchAfter(List.of(after));
    }
    return getCamundaOperateClient().searchProcessInstanceResults(q);
  }

  public List<Variable> getVariables(Long processInstanceKey) throws OperateException {
    return getCamundaOperateClient()
        .searchVariables(
            new SearchQuery.Builder()
                .filter(
                    VariableFilter.builder()
                        .processInstanceKey(processInstanceKey)
                        .scopeKey(processInstanceKey)
                        .build())
                .size(200)
                .build());
  }

  public <T> T getVariable(Long processInstanceKey, String variableName, TypeReference<T> type)
      throws OperateException {
    List<Variable> res =
        getCamundaOperateClient()
            .searchVariables(
                new SearchQuery.Builder()
                    .filter(
                        VariableFilter.builder()
                            .processInstanceKey(processInstanceKey)
                            .name(variableName)
                            .scopeKey(processInstanceKey)
                            .build())
                    .size(1)
                    .build());
    if (res != null && res.size() > 0) {
      return JsonUtils.toParametrizedObject(res.get(0).getValue(), type);
    }
    return null;
  }

  public Map<String, Object> getVariablesAsMap(Long processInstanceKey) throws OperateException {
    return mapVariables(getVariables(processInstanceKey));
  }

  public Map<String, Object> mapVariables(List<Variable> variables) throws OperateException {
    try {
      Map<String, Object> result = new HashMap<>();
      for (Variable var : variables) {
        JsonNode nodeValue = JsonUtils.toJsonNode(var.getValue());
        if (nodeValue.canConvertToLong()) {
          result.put(var.getName(), nodeValue.asLong());
        } else if (nodeValue.isBoolean()) {
          result.put(var.getName(), nodeValue.asBoolean());
        } else if (nodeValue.isTextual()) {
          result.put(var.getName(), nodeValue.textValue());
        } else if (nodeValue.isArray()) {
          result.put(
              var.getName(),
              JsonUtils.toParametrizedObject(var.getValue(), new TypeReference<List<?>>() {}));
        } else {
          result.put(
              var.getName(),
              JsonUtils.toParametrizedObject(
                  var.getValue(), new TypeReference<Map<String, Object>>() {}));
        }
      }
      return result;
    } catch (IOException e) {
      throw new OperateException(e);
    }
  }

  public Map<Long, Map<String, Object>> getVariables(List<ProcessInstance> processInstances)
      throws OperateException {
    try {
      Map<Long, Future<Map<String, Object>>> futures = new HashMap<>();
      Map<Long, Map<String, Object>> instanceMap = new HashMap<>();
      for (ProcessInstance instance : processInstances) {
        futures.put(
            instance.getKey(),
            CompletableFuture.supplyAsync(
                () -> {
                  try {
                    return getVariablesAsMap(instance.getKey());
                  } catch (OperateException e) {
                    return null;
                  }
                }));
      }
      for (Map.Entry<Long, Future<Map<String, Object>>> varFutures : futures.entrySet()) {
        Map<String, Object> vars = varFutures.getValue().get();
        instanceMap.put(varFutures.getKey(), vars);
      }
      futures.clear();
      return instanceMap;
    } catch (ExecutionException | InterruptedException e) {
      throw new OperateException("Error loading instances variables", e);
    }
  }

  public List<Long> getSubProcessInstances(Long processInstanceKey) throws OperateException {
    SearchQuery q =
        new SearchQuery.Builder()
            .filter(
                ProcessInstanceFilter.builder()
                    .parentKey(processInstanceKey)
                    .state(ProcessInstanceState.ACTIVE)
                    .build())
            .size(100)
            .build();

    List<ProcessInstance> subprocs = getCamundaOperateClient().searchProcessInstances(q);
    List<Long> result = new ArrayList<>();
    for (ProcessInstance i : subprocs) {
      result.add(i.getKey());
      result.addAll(getSubProcessInstances(i.getKey()));
    }

    return result;
  }

  public List<FlowNodeInstance> getProcessInstanceHistory(Long processInstanceKey)
      throws OperateException {
    FlowNodeInstanceFilter flowNodeFilter =
        FlowNodeInstanceFilter.builder().processInstanceKey(processInstanceKey).build();
    SearchQuery procInstQuery =
        new SearchQuery.Builder()
            .filter(flowNodeFilter)
            .size(1000)
            .sort(new Sort("startDate", SortOrder.DESC))
            .build();

    return getCamundaOperateClient().searchFlowNodeInstances(procInstQuery);
  }

  public ProcessDefinition getProcessDefinition(Long processDefinitionKey) throws OperateException {
    return getCamundaOperateClient().getProcessDefinition(processDefinitionKey);
  }

  public ProcessInstance getProcessInstance(Long processInstanceKey) throws OperateException {

    return getCamundaOperateClient().getProcessInstance(processInstanceKey);
  }
}
