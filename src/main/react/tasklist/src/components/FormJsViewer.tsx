import React, { useState, useEffect, useLayoutEffect } from 'react';
import { useNavigate } from "react-router-dom";
import { useDispatch, useSelector } from 'react-redux';
import type { } from 'redux-thunk/extend-redux';
import taskService from '../service/TaskService';
import processService from '../service/ProcessService';
import { newForm } from '@camunda-community/form-js-extended-jens';
import { IFormViewer } from '../store/model';
import {Alert } from 'react-bootstrap';

import { useTranslation } from "react-i18next";

function FormJsViewer(formViewer: IFormViewer) {
  const { t } = useTranslation();
  const tasklistConf = useSelector((state: any) => state.process.tasklistConf)
  const navigate = useNavigate();
  const dispatch = useDispatch();
  let errors: string[] = [];
  const [bpmnForm, setBpmnForm] = useState<any | null>(null);
  const docs = useSelector((state: any) => state.documents.docs)
  const missingDocs = useSelector((state: any) => state.documents.missingDocs)

  useEffect(() => {
    const container = document.getElementById(formViewer.id);
    if (container && formViewer.schema) {
      container.innerHTML = '';

      let bpmnForm = newForm({ container: container });
      if (formViewer.disabled) {
        bpmnForm.setProperty('readOnly', true);
      }
      bpmnForm.importSchema(formViewer.schema, formViewer.variables).then(
        function (result: any) {
        });
      setBpmnForm(bpmnForm);
    }
  }, [formViewer]);

  const submit = () => {
    if (bpmnForm != null) {
      bpmnForm.validate();

      for (const field in bpmnForm._getState().errors) {
        if (bpmnForm._getState().errors[field].length > 0) {
          Array.prototype.push.apply(errors, bpmnForm._getState().errors[field]);
        }
      }
      if (errors.length == 0) {
        if (formViewer.variables) {
          if (!tasklistConf.splitPage) {
            dispatch(taskService.submitTask(bpmnForm._getState().data, navigate("/tasklist")));
          } else if (formViewer.id == 'page-form') {
            dispatch(taskService.submitTask(bpmnForm._getState().data, navigate("/tasklist/instances")));
          } else {
            dispatch(taskService.submitTask(bpmnForm._getState().data));
          }
        } else {
          dispatch(processService.instantiate(bpmnForm._getState().data));
        }
      }
    }
  }

  return (
    <div>
      <div id={formViewer.id}></div>
      {missingDocs && missingDocs.length > 0 ?
        <Alert variant="danger">Please provide the missing documents : {missingDocs.map((doc: string, index: number) => <b key={index}>{doc}{index < missingDocs.length-1 ? ', ':''}</b>)}</Alert>
        : <></>}
      <div className="ms-2 me-2 mb-2 d-flex justify-content-between">
        <button disabled={formViewer.disabled || (missingDocs && missingDocs.length > 0)} type="button" className="btn btn-primary" onClick={submit}><i className="bi bi-send"></i> {t("Submit")}</button>
      </div>
    </div>
  )

}

export default FormJsViewer;
