package com.cactusteam.money.data;

import android.content.res.Resources;

import com.cactusteam.money.R;
import com.cactusteam.money.app.MoneyApp;
import com.cactusteam.money.data.dao.Account;
import com.cactusteam.money.data.dao.Category;
import com.cactusteam.money.data.dao.DaoSession;
import com.cactusteam.money.data.dao.Subcategory;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author vpotapenko
 */
public class DbInitializer {

    public void execute(DaoSession daoSession) throws IOException, JSONException {
        Resources resources = MoneyApp.Companion.getInstance().getResources();
        InputStream inputStream = resources.openRawResource(R.raw.initialize_data);

        try {
            String json = IOUtils.toString(inputStream);

            JSONObject object = new JSONObject(json);

            createAccounts(daoSession, object.getJSONArray("accounts"));
            createCategories(daoSession, object.getJSONArray("categories"));
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }

    private void createCategories(DaoSession daoSession, JSONArray categories) throws JSONException {
        for (int i = 0; i < categories.length(); i++) {
            createCategory(daoSession, categories.getJSONObject(i));
        }
    }

    private void createCategory(DaoSession daoSession, JSONObject jsonObject) throws JSONException {
        Category category = new Category();
        category.setName(jsonObject.getString("name"));
        category.setIcon(jsonObject.optString("icon", null));

        int type = jsonObject.getInt("type");
        category.setType(type == 0 ? Category.EXPENSE : Category.INCOME);

        daoSession.insert(category);

        if (jsonObject.has("subcategories")) {
            JSONArray array = jsonObject.getJSONArray("subcategories");
            for (int i = 0; i < array.length(); i++) {
                Subcategory subcategory = new Subcategory();
                subcategory.setName(array.getString(i));
                subcategory.setCategoryId(category.getId());

                daoSession.insert(subcategory);
            }

            category.resetSubcategories();
            category.getSubcategories();
        }
    }

    private void createAccounts(DaoSession daoSession, JSONArray accounts) throws JSONException {
        String currencyCode = MoneyApp.Companion.getInstance().getAppPreferences().getMainCurrencyCode();
        for (int i = 0; i < accounts.length(); i++) {
            createAccount(daoSession, accounts.getJSONObject(i), currencyCode);
        }
    }

    private void createAccount(DaoSession daoSession, JSONObject jsonObject, String currencyCode) throws JSONException {
        Account account = new Account();
        account.setName(jsonObject.getString("name"));
        account.setColor(jsonObject.getString("color"));
        account.setCurrencyCode(currencyCode);

        daoSession.insert(account);
    }
}
