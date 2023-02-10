package com.sccs.api.mypage.mapper;

import java.util.HashMap;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MypageMapper {

  public List<HashMap<String, Object>> getHistory(String memberId, String year, String month);

  public HashMap<String, Object> getHistoryDetail(int studyId);

  public List<HashMap<String, Object>> getProblems(String memberId);

  public String getProblemUrl(int problemId);

  public HashMap<String, Object> getProblemIdAndUrl(int submissionId);

}
