import React, { useState, useEffect, useMemo } from 'react'
import styled from 'styled-components'
import { Resizable } from 're-resizable'
import { useSelector } from 'react-redux'
import { useParams, useNavigate } from 'react-router-dom'
import { useWindowHeight } from 'hooks/useWindowHeight'
import api from 'constants/api'
import { languagePk } from 'constants/pk'
import axios from 'libs/axios'
import Code from 'components/study/Code'
import Loading from 'components/common/Loading'
import ProblemImage from 'components/common/ProblemImage'
import ResultSection from 'components/study/ResultSection'
import ButtonDropdown from 'components/common/ButtonDropdown'

export default function SolvePage() {
  const navigate = useNavigate()

  const memberId = useSelector((state) => state.user.id)
  const { problemId } = useParams()

  // window의 innerHeight를 반환하는 커스텀 훅
  const windowHeight = useWindowHeight()

  const [problemImageUrl, setProblemImageUrl] = useState(null)
  const [submissionList, setSubmissionList] = useState([])
  const [code, setCode] = useState('')
  const [languageId, setLanguageId] = useState(1)
  const [testResult, setTestResult] = useState(null)

  // 문제 번호 클릭 시, 제출내역 저장
  useEffect(() => {
    const [url, method] = api('solveProblem', { memberId, problemId })
    const config = { url, method }
    axios
      .request(config)
      .then((res) => {
        setProblemImageUrl(res.data.problemUrl)
        setSubmissionList(res.data.submissionInfo)
      })
      .catch((err) => {
        alert('스터디 내역을 불러오지 못했습니다.')
      })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // 코드 제출
  const submitCode = (type) => {
    const apiKey = type === 'submit' ? 'submitCode' : 'testCode'
    const codeString = code
    const formData = new FormData()
    formData.append('formFile', new Blob([codeString], { type: 'text/plain' }))
    formData.append('memberId', memberId)
    formData.append('problemId', problemId)
    formData.append('languageId', languageId)
    const headers = { 'Content-Type': 'multipart/form-data' }
    const [url, method] = api(apiKey)
    const config = { url, method, data: formData, headers }
    axios(config)
      .then((res) => {
        setTestResult(res.data)
      })
      .catch(() => {})
  }

  // 제출한 코드 목록 가공
  const codesObject = useMemo(() => {
    const tempObject = {}
    submissionList.forEach((code, idx) => {
      tempObject[`${idx}`] = `${idx + 1}번째 제출코드` //code.submissionFile
    })

    return tempObject
  }, [submissionList])

  // 선택한 제출내역의 코드를 string으로 fetch
  const fetchData = async (idx) => {
    fetch(submissionList[idx[0]].submissionFile)
      .then((res) => {
        return res.text()
      })
      .then((code) => {
        // console.log('code', code)
        setCode(code)
      })
  }

  // 언어선택 드롭다운을 통해 languageId 바꾸기
  const changeLanguageId = (idx) => {
    console.log(idx[0])
    setLanguageId(idx[0])
  }

  return (
    <>
      {problemImageUrl ? (
        <Container>
          <ImageWrapper windowHeight={windowHeight}>
            <ProblemImage src={problemImageUrl} />
          </ImageWrapper>
          <Resizable
            defaultSize={{ width: '50%', height: '100%' }}
            minWidth={'20%'}
            maxWidth={'80%'}
            enable={{
              top: false,
              right: true,
              bottom: false,
              left: true,
              topRight: false,
              bottomRight: false,
              bottomLeft: false,
              topLeft: false,
            }}
          >
            <FlexColumn windowHeight={windowHeight}>
              <FlexBox>
                <ButtonDropdown
                  title="언어 선택"
                  size="small"
                  type="primary"
                  options={languagePk}
                  onClick={(e) => changeLanguageId(e.target.id.split('-'))}
                />
                <ButtonDropdown
                  title="코드 선택"
                  size="small"
                  type="primary"
                  options={codesObject}
                  onClick={(e) => fetchData(e.target.id.split('-'))}
                />
              </FlexBox>
              <Code value={code} setValue={setCode} languageId={languageId} />
              <Resizable
                defaultSize={{ width: '100%', height: '40%' }}
                minHeight={'20%'}
                maxHeight={'80%'}
                enable={{
                  top: true,
                  right: false,
                  bottom: false,
                  left: false,
                  topRight: false,
                  bottomRight: false,
                  bottomLeft: false,
                  topLeft: false,
                }}
              >
                <ResultSection
                  results={testResult}
                  finish={() => {
                    navigate(-1)
                  }}
                  test={() => {
                    submitCode('test')
                  }}
                  submit={() => {
                    submitCode('submit')
                  }}
                />
              </Resizable>
            </FlexColumn>
          </Resizable>
        </Container>
      ) : (
        <Loading height="70vh" />
      )}
    </>
  )
}

const Container = styled.div`
  display: flex;
  gap: 5px;

  height: 100vh;

  padding: 1rem;

  background-color: ${({ theme }) => theme.studyBaseBgColor};
`
const FlexBox = styled.div`
  display: flex;
  justify-content: space-between;
`

const FlexColumn = styled.div`
  display: flex;
  flex-direction: column;
  gap: 5px;
  height: ${({ windowHeight }) => `calc(${windowHeight}px - 30px)`};
`
const ImageWrapper = styled.div`
  padding: 0.5rem;
  border-radius: 0.5rem;
  height: ${({ windowHeight }) => `calc(${windowHeight}px - 30px)`};
  background-color: ${({ theme }) => theme.whiteColor};
`
