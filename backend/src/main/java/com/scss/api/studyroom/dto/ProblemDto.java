package com.scss.api.studyroom.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class ProblemDto {
<<<<<<< HEAD
    privatte int id;
=======
    private int id;
>>>>>>> 573af66ceb3eca469c47996fd6c1b8ebe79e5012
    private String name;
    private int difficulty;
    private String problemFolder;
    private int memoryLimit ;
    private int timeLimit;
    private int submissionNumber;
    private int acceptedNumber;
    private int algoId;
    
}
