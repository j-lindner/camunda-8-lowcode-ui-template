import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import adminFormService from '../service/AdminFormService';

function AdminFormEdit() {
  const dispatch = useDispatch();
  const form = useSelector((state: any) => state.adminForms.currentForm)


  useEffect(() => {
    dispatch(adminFormService.buildEditor(document.querySelector('#form-editor'), form));
  }, [form]);

  return (
    <div id="form-editor"></div>
  );
}

export default AdminFormEdit
