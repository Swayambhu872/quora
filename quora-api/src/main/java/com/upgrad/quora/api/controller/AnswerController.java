package com.upgrad.quora.api.controller;

import com.upgrad.quora.api.model.*;
import com.upgrad.quora.service.business.AnswerService;
import com.upgrad.quora.service.business.QuestionService;
import com.upgrad.quora.service.business.UserAuthenticationService;
import com.upgrad.quora.service.entity.AnswerEntity;
import com.upgrad.quora.service.entity.QuestionEntity;
import com.upgrad.quora.service.entity.UserAuthEntity;
import com.upgrad.quora.service.entity.UserEntity;
import com.upgrad.quora.service.exception.AnswerNotFoundException;
import com.upgrad.quora.service.exception.AuthorizationFailedException;
import com.upgrad.quora.service.exception.InvalidQuestionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/")
public class AnswerController {
    @Autowired
    private AnswerService answerService;
    @Autowired
    private UserAuthenticationService userAuthenticationService;
    @Autowired
    private QuestionService questionService;

    @RequestMapping(method = RequestMethod.POST, path = "/question/{questionId}/answer/create", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AnswerResponse> createAnswer(@RequestHeader("authorization") final String authorization,
         @PathVariable("questionId") final String questionId, final AnswerRequest answerRequest) throws AuthorizationFailedException, InvalidQuestionException {
        final UserAuthEntity userAuthEntity = userAuthenticationService.getUser(authorization);
        QuestionEntity questionEntity = questionService.validateQuestion(questionId);

        UserEntity userEntity = userAuthEntity.getUserEntity();
        AnswerEntity answerEntity = new AnswerEntity();
        answerEntity.setUuid(UUID.randomUUID().toString());
        answerEntity.setDate(ZonedDateTime.now());
        answerEntity.setQuestion(questionEntity);
        answerEntity.setAnswer(answerRequest.getAnswer());
        answerEntity.setUser(userEntity);

        AnswerEntity createdAnswer = answerService.createAnswer(answerEntity, userAuthEntity);
        AnswerResponse answerResponse = new AnswerResponse().id(createdAnswer.getUuid()).status("ANSWER CREATED");

        return new ResponseEntity<AnswerResponse>(answerResponse, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, path = "/answer/edit/{answerId}", consumes = MediaType.APPLICATION_JSON_UTF8_VALUE, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AnswerEditResponse> editAnswer(@RequestHeader("authorization") final String authorization,
                                                         @PathVariable("answerId") final String answerId,
                                                         final AnswerEditRequest answerEditRequest)throws AuthorizationFailedException, AnswerNotFoundException {
        UserAuthEntity userAuthEntity = userAuthenticationService.getUser(authorization);
        AnswerEntity answerEntity = answerService.getAnswerFromId(answerId);
        AnswerEntity verifiedAnsEdit = answerService.verifyAnsUserToEdit(userAuthEntity, answerEntity);
        verifiedAnsEdit.setAnswer(answerEditRequest.getContent());
        AnswerEntity newAns = answerService.newAns(verifiedAnsEdit);
        AnswerEditResponse answerEditResponse = new AnswerEditResponse().id(newAns.getUuid()).status("ANSWER EDITED");

        return new ResponseEntity<AnswerEditResponse>(answerEditResponse, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.DELETE, path = "/answer/delete/{answerId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<AnswerDeleteResponse> deleteAnswer(@RequestHeader("authorization") final String authorization,
                                                             @PathVariable("answerId") final String answerId)throws AuthorizationFailedException, AnswerNotFoundException {
        UserAuthEntity userAuthEntity = userAuthenticationService.getUser(authorization);
        UserEntity userEntity = userAuthEntity.getUserEntity();
        AnswerEntity answerEntity = answerService.getAnswerFromId(answerId);
        AnswerEntity verifiedAnsDelete;
        if (userEntity.getRole().equalsIgnoreCase("admin")) {
            verifiedAnsDelete = answerEntity;
        } else {
            verifiedAnsDelete = answerService.verifyAnsUserToDelete(userAuthEntity, answerEntity);
        }
        AnswerEntity deleteAnswer = answerService.deleteAnswer(verifiedAnsDelete);
        AnswerDeleteResponse answerDeleteResponse = new AnswerDeleteResponse().id(deleteAnswer.getUuid()).status("ANSWER DELETED");

        return new ResponseEntity<AnswerDeleteResponse>(answerDeleteResponse,HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, path = "answe/all/{questionId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<List<AnswerDetailsResponse>> getAllAnswersToQuestion(@RequestHeader("authorization") final String authorization,
                                                                               @PathVariable("questionId") final String questionId)throws AuthorizationFailedException, InvalidQuestionException {
        UserAuthEntity userAuthEntity = userAuthenticationService.getUser(authorization);
        QuestionEntity questionEntity = questionService.validateQuestion(questionId);

        ArrayList<AnswerDetailsResponse> list = null;
        ArrayList<AnswerEntity> rawlist = (ArrayList<AnswerEntity>) answerService.getAllAnswersToQuestion(questionId, userAuthEntity);
        for(AnswerEntity ans : rawlist) {
            AnswerDetailsResponse detailsResponse = new AnswerDetailsResponse();
            detailsResponse.setId(ans.getUuid());
            detailsResponse.setAnswerContent(ans.getAnswer());
            detailsResponse.setQuestionContent(questionEntity.getContent());
            list.add(detailsResponse);
        }
        return new ResponseEntity<List<AnswerDetailsResponse>>(list, HttpStatus.OK);
    }
}
