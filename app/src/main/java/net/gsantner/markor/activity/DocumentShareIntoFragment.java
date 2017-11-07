/*
 * Copyright (c) 2017 Gregor Santner and Markor contributors
 *
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */
package net.gsantner.markor.activity;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.TextView;

import net.gsantner.markor.R;
import net.gsantner.markor.dialog.FilesystemDialogCreator;
import net.gsantner.markor.model.Document;
import net.gsantner.markor.model.DocumentLoader;
import net.gsantner.markor.renderer.MarkDownRenderer;
import net.gsantner.markor.ui.BaseFragment;
import net.gsantner.markor.util.AppSettings;
import net.gsantner.markor.util.PermissionChecker;
import net.gsantner.opoc.ui.FilesystemDialogData;
import net.gsantner.opoc.util.ActivityUtils;

import java.io.File;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DocumentShareIntoFragment extends BaseFragment {
    public static final String FRAGMENT_TAG = "DocumentShareIntoFragment";
    public static final String EXTRA_SHARED_TEXT = "EXTRA_SHARED_TEXT";

    public static DocumentShareIntoFragment newInstance(String sharedText) {
        DocumentShareIntoFragment f = new DocumentShareIntoFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_SHARED_TEXT, sharedText);
        f.setArguments(args);
        return f;
    }

    @BindView(R.id.document__fragment__share_into__webview)
    WebView _webView;

    private View _view;
    private Context _context;
    private String _sharedText;

    public DocumentShareIntoFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.document__fragment__share_into, container, false);
        ButterKnife.bind(this, view);
        _view = view;
        _context = view.getContext();
        _sharedText = getArguments() != null ? getArguments().getString(EXTRA_SHARED_TEXT, "") : "";
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        int fg = AppSettings.get().isDarkThemeEnabled() ? Color.WHITE : Color.BLACK;

        _view.setBackgroundColor(fg == Color.WHITE ? Color.BLACK : Color.WHITE);
        for (int resid : new int[]{R.id.document__fragment__share_into__append_to_document, R.id.document__fragment__share_into__create_document, R.id.document__fragment__share_into__append_to_quicknote}) {
            LinearLayout layout = _view.findViewById(resid);
            ((TextView) (layout.getChildAt(1))).setTextColor(fg);
        }

        Document document = new Document();
        document.setContent(_sharedText);
        String html = MarkDownRenderer.renderMarkdownIntoWebview(document, _webView);
    }


    @OnClick({R.id.document__fragment__share_into__append_to_document, R.id.document__fragment__share_into__create_document, R.id.document__fragment__share_into__append_to_quicknote})
    public void onClick(View view) {
        ActivityUtils au = new ActivityUtils(getActivity());
        switch (view.getId()) {
            case R.id.document__fragment__share_into__create_document: {
                if (PermissionChecker.doIfPermissionGranted(getActivity())) {
                    createNewDocument();
                }
                break;
            }
            case R.id.document__fragment__share_into__append_to_document: {
                if (PermissionChecker.doIfPermissionGranted(getActivity())) {
                    showAppendDialog();
                }
                break;
            }
            case R.id.document__fragment__share_into__append_to_quicknote: {
                if (PermissionChecker.doIfPermissionGranted(getActivity())) {
                    appendToExistingDocument(AppSettings.get().getQuickNote(), false);
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                }

            }
        }
    }

    private void showAppendDialog() {
        FilesystemDialogCreator.showFileDialog(new FilesystemDialogData.SelectionListenerAdapter() {
            @Override
            public void onFsDialogConfig(FilesystemDialogData.Options opt) {
                opt.rootFolder = new File(AppSettings.get().getSaveDirectory());
                opt.titleText = R.string.append_to_document;
            }

            @Override
            public void onFsSelected(String request, File file) {
                appendToExistingDocument(file, true);
            }

        }, getFragmentManager(), getActivity());
    }

    private void appendToExistingDocument(File file, boolean showEditor) {
        Bundle args = new Bundle();
        args.putSerializable(DocumentLoader.EXTRA_PATH, file);
        args.putBoolean(DocumentLoader.EXTRA_PATH_IS_FOLDER, false);
        Document document = DocumentLoader.loadDocument(_context, args, null);
        String prepend = TextUtils.isEmpty(document.getContent()) ? "" : (document.getContent() + "\n");
        DocumentLoader.saveDocument(document, false, prepend + _sharedText);
        if (showEditor) {
            showInDocumentActivity(document);
        }
    }

    private void createNewDocument() {
        // Create a new document
        Bundle args = new Bundle();
        args.putSerializable(DocumentLoader.EXTRA_PATH, new File(AppSettings.get().getSaveDirectory()));
        args.putBoolean(DocumentLoader.EXTRA_PATH_IS_FOLDER, true);
        Document document = DocumentLoader.loadDocument(_context, args, null);
        DocumentLoader.saveDocument(document, false, _sharedText);

        // Load document as file
        args.putSerializable(DocumentLoader.EXTRA_PATH, document.getFile());
        args.putBoolean(DocumentLoader.EXTRA_PATH_IS_FOLDER, false);
        document = DocumentLoader.loadDocument(_context, args, null);
        document.setTitle("");
        showInDocumentActivity(document);
    }

    private void showInDocumentActivity(Document document) {
        if (getActivity() instanceof DocumentActivity) {
            DocumentActivity a = (DocumentActivity) getActivity();
            a.setDocument(document);
            if (AppSettings.get().isPreviewFirst()) {
                a.showPreview(document, null);
            } else {
                a.showEditor(document, null, false);
            }
        }

    }

    @Override
    public String getFragmentTag() {
        return FRAGMENT_TAG;
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }
}
