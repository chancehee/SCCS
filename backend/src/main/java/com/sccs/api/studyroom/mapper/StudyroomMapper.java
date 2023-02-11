package com.sccs.api.studyroom.mapper;

import com.sccs.api.member.dto.MemberDto;
import com.sccs.api.studyroom.dto.ProblemDto;
import com.sccs.api.studyroom.dto.StudyroomAlgoDto;
import com.sccs.api.studyroom.dto.StudyroomDto;
import com.sccs.api.studyroom.dto.StudyroomLanguageDto;
import com.sccs.api.studyroom.dto.StudyroomMemberDto;
import com.sccs.api.studyroom.dto.StudyroomProblemDto;
import com.sccs.api.studyroom.dto.SubmissionDto;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StudyroomMapper {

  public void createStudyroom(StudyroomDto studyroomDto);

  public void insertLanguageId(StudyroomLanguageDto studyroomLanguageDto);

  public void insertAlgoId(StudyroomAlgoDto studyroomAlgoDto);

  public int selectProblemCount(int algoId);

  public void insertProblemId(StudyroomProblemDto studyroomProblemDto);

  public int selectProblemId(String path);

  public void insertMemberId(StudyroomMemberDto studyroomMemberDto);

  public List<StudyroomDto> selectAllStudyroom();

  public List<StudyroomDto> selectStudyroom(StudyroomDto studyroomDto);

  public int checkStudyroomPassword(StudyroomDto studyroomDto);

  public void insertMemberIds(StudyroomDto studyroomDto);

  public StudyroomDto selectStudyroomById(int id);

  public List<ProblemDto> selectProblemByStudyroomId(int id);

  public void submitProblem(SubmissionDto submissionDto);
  public void submitTest(SubmissionDto submissionDto);

  public void changeStudyroomSolvingStatus(StudyroomDto studyroomDto);

  public int endStudyroomByOwner(int id);

  public ProblemDto getProblemInfo(int problemId);

  public StudyroomDto enterStudyroom(int id);

  public int getStudyroomPersonnel(int id);

  public int increaseStudyroomPersonnel(StudyroomDto studyroomDto);

  public int decreaseStudyroomPersonnel(StudyroomDto studyroomDto);

  public boolean isExistStudyroom(int id);

  public List<SubmissionDto> getStudyInfo(StudyroomDto studyroomDto);

  public String getNicknameById(String id);

  public MemberDto getHostnicknameByStudyroomInfo(int studyroomId);

  public int isSolvingByUser(SubmissionDto submissionDto);

  public int getScoreByGrade(Map<String, Object> resultMap);

  public int getScoreByMember(String id);

  public void injectScore(Map<String, Object> resultMap);
}
