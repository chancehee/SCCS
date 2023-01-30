package com.scss.api.studyroom.service;

import com.scss.api.studyroom.dto.*;
import com.scss.api.studyroom.mapper.StudyroomMapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudyroomServiceImpl implements StudyroomService{
    private static final Logger logger = LoggerFactory.getLogger(StudyroomServiceImpl.class);
    private static final String SUCCESS = "success";
    private static final String FAIL = "fail";
    // 생성자 주입
    private final StudyroomMapper studyroomMapper;

    // 방 생성
    @Override
    public String createStudyroom(StudyroomDto studyroomDto) {
        try {

            // 스터디 룸 생성
            studyroomMapper.createStudyroom(studyroomDto);
            System.out.println("during");
            // studuroom_language 에 언어 유형 입력
            int id = studyroomDto.getId();
            List<Integer> language_ids =  studyroomDto.getLanguageIds();

            StudyroomLanguageDto studyroomLanguageDto = new StudyroomLanguageDto();
            studyroomLanguageDto.setStudyroomId(id);

            for(int i=0; i<language_ids.size(); i++){
                studyroomLanguageDto.setLanguageId(language_ids.get(i));
                studyroomMapper.insertLanguageId(studyroomLanguageDto);
            }

            List<Integer> algo_ids = studyroomDto.getAlgoIds();
            StudyroomAlgoDto studyroomAlgoDto = new StudyroomAlgoDto();
            studyroomAlgoDto.setStudyroomId(id);
            //받은 알고리즘 유형이 랜덤이면
            if(algo_ids.size()==1 && algo_ids.get(0).equals(0)){
                int min = 1;
                int max = 2;

                for(int i=0; i<2; i++){
                    // 알고리즘 유형 2개 랜덤하게 고르기
                    int randomAlgo = (int)(Math.random() * (max - min + 1)) + min;
                    // 알고리즘 유형 2개 저장.
                    studyroomAlgoDto.setAlgoId(randomAlgo);
                    studyroomMapper.insertAlgoId(studyroomAlgoDto);
                    // 저장한 2개 알고리즘 유형 중에서 문제 랜덤으로 2개 선택하기
                    int algoCount = studyroomMapper.selectProblemCount(studyroomAlgoDto.getAlgoId());
                    int min2 = 1;
                    int max2 = algoCount;
                    int randomProblem = (int)(Math.random() * (max2 - min2 + 1)) + min2;
                    String path =Integer.toString(randomAlgo)+"/"+Integer.toString(randomProblem);
                    int problemId = studyroomMapper.selectProblemId(path);
                    StudyroomProblemDto studyroomProblemDto = new StudyroomProblemDto();
                    studyroomProblemDto.setStudyroomId(id);
                    studyroomProblemDto.setProblemId(problemId);
                    studyroomMapper.insertProblemId(studyroomProblemDto);

                }
            }else if(algo_ids.size()==1 && !algo_ids.get(0).equals(0)){
                // 알고리즘 유형 1개 저장
                studyroomAlgoDto.setAlgoId(algo_ids.get(0));
                studyroomMapper.insertAlgoId(studyroomAlgoDto);
                // 저장한 1개 알고리즘 유형 중에서 문제 랜덤으로 2개 선택하기

                int algoCount = studyroomMapper.selectProblemCount(studyroomAlgoDto.getAlgoId());
                int n[] = new int[2];
                int randomProblem = 0;
                for(int i = 0; i<n.length;i++) {
                    do {
                        int min2 = 1;
                        int max2 = algoCount;
                        randomProblem = (int)(Math.random() * (max2 - min2 + 1)) + min2;
                    }while(checkout(n,randomProblem));
                    n[i] = randomProblem;
                }
                StudyroomProblemDto studyroomProblemDto = new StudyroomProblemDto();
                studyroomProblemDto.setStudyroomId(id);
                for(int i = 0; i<2; i++) {
                    String path =Integer.toString(algo_ids.get(0))+"/"+Integer.toString(n[i]);
                    int problemId = studyroomMapper.selectProblemId(path);
                    studyroomProblemDto.setProblemId(problemId);
                    studyroomMapper.insertProblemId(studyroomProblemDto);
                }

            }else if(algo_ids.size()==2){
                // 알고리즘 유형 2개 저장
                // 저장한 2개 알고리즘 유형 중에서 각각 하나씩 문제 랜덤으로 2개 선택하기
                for(int i=0; i<2; i++){
                    studyroomAlgoDto.setAlgoId(algo_ids.get(i));
                    studyroomMapper.insertAlgoId(studyroomAlgoDto);
                    int algoCount = studyroomMapper.selectProblemCount(studyroomAlgoDto.getAlgoId());
                    int min2 = 1;
                    int max2 = algoCount;
                    int randomProblem = (int)(Math.random() * (max2 - min2 + 1)) + min2;
                    StudyroomProblemDto studyroomProblemDto = new StudyroomProblemDto();
                    String path =Integer.toString(algo_ids.get(i))+"/"+Integer.toString(randomProblem);
                    int problemId = studyroomMapper.selectProblemId(path);
                    studyroomProblemDto.setStudyroomId(id);
                    studyroomProblemDto.setProblemId(problemId);
                    studyroomMapper.insertProblemId(studyroomProblemDto);
                }
            }

            // 방장을 알고리즘 스터디_멤버에 삽입
            StudyroomMemberDto studyroomMemberDto = new StudyroomMemberDto();
            studyroomMemberDto.setMemberId(studyroomDto.getMemberId());
            studyroomMemberDto.setStudyroomId(id);
            studyroomMapper.insertMemberId(studyroomMemberDto);
            return SUCCESS;
        } catch (Exception e) {
            e.printStackTrace();
            return FAIL;
        }
    }


    @Override
    public List<Map<String, Object>> selectAllStudyroom() {
        List<StudyroomDto> s = studyroomMapper.selectAllStudyroom();
        List<Map<String, Object>> studyrooms = new ArrayList<>();
        int size = s.size();

        for(int i=0; i<size; i++){
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put( "languageIds", s.get(i).getLanguageIds() );
            resultMap.put( "algoIds", s.get(i).getAlgoIds() );
            resultMap.put("isPrivate", s.get(i).getIsPrivate());
            resultMap.put("isSolving", s.get(i).getIsSolving());
            resultMap.put("title", s.get(i).getTitle());
            resultMap.put("id", s.get(i).getId());
            studyrooms.add(resultMap);
        }
        return studyrooms;

    }


    @Override
    public List<Map<String, Object>> selectStudyroom(StudyroomDto studyroomDto) {
        List<StudyroomDto> s = studyroomMapper.selectStudyroom(studyroomDto);
        List<Map<String, Object>> studyrooms = new ArrayList<>();
        int size = s.size();

        for(int i=0; i<size; i++){
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put( "languageIds", s.get(i).getLanguageIds() );
            resultMap.put( "algoIds", s.get(i).getAlgoIds() );
            resultMap.put("isPrivate", s.get(i).getIsPrivate());
            resultMap.put("isSolving", s.get(i).getIsSolving());
            resultMap.put("title", s.get(i).getTitle());
            resultMap.put("id", s.get(i).getId());
            studyrooms.add(resultMap);
        }
            return studyrooms;

    }

    @Override
    public String checkStudyroomPassword(StudyroomDto studyroomDto) {
        try {
            if(studyroomMapper.checkStudyroomPassword(studyroomDto)==1){
                return SUCCESS;
            }else{
                return FAIL;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return FAIL;
        }
    }

    @Override
    public Map<String, Object> startCodingTest(StudyroomDto studyroomDto) {

        Map<String, Object> resultMap = new HashMap<>();
        // type을 진행 중으로 바꾼다.
        studyroomMapper.changeStudyroomSolvingStatus(studyroomDto);

        // 스터디 시작하는 애들 아이디 넣어준다.
        studyroomMapper.insertMemberIds(studyroomDto);
        System.out.println("여기 까지 진행 됨");
        // 스터디룸 정보를 담은 걸 resultmap에 담는다.
        StudyroomDto s = studyroomMapper.selectStudyroomById(studyroomDto.getId());
        resultMap.put( "algo_ids", s.getAlgoIds());
        resultMap.put("title", s.getTitle());
        resultMap.put("id", s.getId());

        // 스터디룸에 맞는 문제를 resultmap에 담는다.
        List<ProblemDto> p = studyroomMapper.selectProblemByStudyroomId(studyroomDto.getId());
        resultMap.put( "problems", p);
        return resultMap;

    }

    @Override
    public void submitProblem(SubmissionDto submissionDto) {
        studyroomMapper.submitProblem(submissionDto);
    }

    @Override
    public String endStudyroomByOwner(StudyroomDto studyroomDto) {
        try {
            if(studyroomMapper.endStudyroomByOwner(studyroomDto)==1){
                return SUCCESS;
            }else{
                return FAIL;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return FAIL;
        }


    }


    private static boolean checkout(int n[], int index) {
        for (int i = 0; i < n.length; i++) {
            if(n[i] == index)
                return true;
        }
        return false;
    }

}