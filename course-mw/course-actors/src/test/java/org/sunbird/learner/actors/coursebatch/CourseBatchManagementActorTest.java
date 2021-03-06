package org.sunbird.learner.actors.coursebatch;

import static org.powermock.api.mockito.PowerMockito.when;

import akka.dispatch.Futures;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.application.test.SunbirdApplicationActorTest;
import org.sunbird.builder.mocker.CassandraMocker;
import org.sunbird.builder.mocker.ESMocker;
import org.sunbird.builder.mocker.MockerBuilder;
import org.sunbird.builder.mocker.UserOrgMocker;
import org.sunbird.builder.object.CustomObjectBuilder;
import org.sunbird.common.factory.EsClientFactory;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.request.Request;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.EkStepRequestUtil;
import org.sunbird.userorg.UserOrgServiceImpl;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class})
@PowerMockIgnore("javax.management.*")
public class CourseBatchManagementActorTest extends SunbirdApplicationActorTest {

  private MockerBuilder.MockersGroup group;

  public CourseBatchManagementActorTest() {
    init(CourseBatchManagementActor.class);
  }

  @Test
  @PrepareForTest({
    ServiceFactory.class,
    EsClientFactory.class,
    UserOrgServiceImpl.class,
    EkStepRequestUtil.class
  })
  public void createBatchInviteSuccess() {
    group =
        MockerBuilder.getFreshMockerGroup()
            .withCassandraMock(new CassandraMocker())
            .withESMock(new ESMocker())
            .withUserOrgMock(new UserOrgMocker())
            .andStaticMock(EkStepRequestUtil.class);
    Map<String, Object> courseBatch =
        CustomObjectBuilder.getCourseBatchBuilder()
            .generateRandomFields()
            .addField(JsonKey.ENROLLMENT_TYPE, JsonKey.INVITE_ONLY)
            .build()
            .get();
    when(group
            .getESMockerService()
            .save(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(Futures.successful("randomESID"));
    when(EkStepRequestUtil.searchContent(Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(CustomObjectBuilder.getRandomCourse().get());
    when(group
            .getCassandraMockerService()
            .insertRecord(Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(
            new CustomObjectBuilder.CustomObjectWrapper<Boolean>(true).asCassandraResponse());
    when(group.getUserOrgMockerService().getOrganisationById(Mockito.anyString()))
        .thenReturn(CustomObjectBuilder.getRandomOrg().get());
    String orgId = ((List<String>) courseBatch.get(JsonKey.COURSE_CREATED_FOR)).get(0);
    when(group.getUserOrgMockerService().getUsersByIds(Mockito.anyList()))
        .then(
            new Answer<List<Map<String, Object>>>() {
              @Override
              public List<Map<String, Object>> answer(InvocationOnMock invocation)
                  throws Throwable {
                List<String> userList = (List<String>) invocation.getArguments()[0];
                return CustomObjectBuilder.getRandomUsersWithIds(userList, orgId).get();
              }
            });
    when(group.getUserOrgMockerService().getUserById(Mockito.anyString()))
        .then(
            new Answer<Map<String, Object>>() {

              @Override
              public Map<String, Object> answer(InvocationOnMock invocation) throws Throwable {
                String userId = (String) invocation.getArguments()[0];
                return CustomObjectBuilder.getRandomUsersWithIds(Arrays.asList(userId), orgId)
                    .get()
                    .get(0);
              }
            });
    Request req = new Request();
    req.setOperation("createBatch");
    req.setRequest(courseBatch);
    Response response = executeInTenSeconds(req, Response.class);
    Assert.assertNotNull(response);
    Assert.assertNotNull(response.get(JsonKey.BATCH_ID));
  }

  @Test
  @PrepareForTest({EsClientFactory.class})
  public void getBatchSuccess() {
    group = MockerBuilder.getFreshMockerGroup().withESMock(new ESMocker());
    when(group.getESMockerService().getDataByIdentifier(Mockito.anyString(), Mockito.anyString()))
        .thenReturn(CustomObjectBuilder.getRandomCourseBatch().asESIdentifierResult());
    Request req = new Request();
    req.setOperation("getBatch");
    req.getContext().put(JsonKey.BATCH_ID, "randomBatchId");
    Response response = executeInTenSeconds(req, Response.class);
    Assert.assertNotNull(response);
  }
}
