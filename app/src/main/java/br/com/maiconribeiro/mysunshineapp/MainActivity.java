package br.com.maiconribeiro.mysunshineapp;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import br.com.maiconribeiro.mysunshineapp.sync.BuscarCidadesPorNome;
import br.com.maiconribeiro.mysunshineapp.sync.BuscarDadosMeteorologicos;

public class MainActivity extends AppCompatActivity {

    private ListView listViewPrevisaoTempo;
    private ArrayAdapter<String> arrayAdapter;
    private final Locale myLocale = new Locale("pt", "BR");
    private SearchView searchView;

    private String localidade;
    private String metrica;

    private ArrayList<String> listaPrevisaoTempo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        listViewPrevisaoTempo = (ListView) findViewById(R.id.listview_forecast);

        this.obterPreferenciasUsuario();

        //Método de pesquisa previsão do tempo baseado na localidade
        this.pesquisarPrevisaoTempo(localidade, metrica);

        listViewPrevisaoTempo.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // ListView Clicked item value
                String itemValue = arrayAdapter.getItem(position);

                Intent intent = new Intent(MainActivity.this, DetalhesTempoActivity.class);
                Bundle params = new Bundle();
                params.putString("detalhes", itemValue);
                intent.putExtras(params);
                startActivity(intent);

            }
        });

        //Seta o titulo da ActionBar com o nome da cidade escolhida
        getSupportActionBar().setTitle(localidade);

        //Torna a ActionBar trasparente
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION, WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#330000ff")));
        getSupportActionBar().setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#550000ff")));

    }

    @Override
    protected void onResume() {

        this.obterPreferenciasUsuario();

        this.pesquisarPrevisaoTempo(localidade, metrica);

        getSupportActionBar().setTitle(localidade);

        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem menuItem = menu.findItem(R.id.action_search);

        searchView = (SearchView) MenuItemCompat.getActionView(menuItem);

        searchView.setSuggestionsAdapter(new SimpleCursorAdapter(
                this, android.R.layout.simple_list_item_1, null,
                new String[]{SearchManager.SUGGEST_COLUMN_TEXT_1},
                new int[]{android.R.id.text1}));

        searchView.setOnQueryTextListener(onSearch());
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {

            @Override
            public boolean onSuggestionSelect(int position) {

                Cursor cursor = (Cursor) searchView.getSuggestionsAdapter().getItem(position);
                localidade = cursor.getString(cursor.getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1));
                cursor.close();

                searchView.setQuery(localidade, false);
                pesquisarPrevisaoTempo(localidade, metrica);

                View view = getCurrentFocus();
                if (view != null) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                }

                return true;
            }

            @Override
            public boolean onSuggestionClick(int position) {

                return onSuggestionSelect(position);
            }
        });


        return true;
    }

    private SearchView.OnQueryTextListener onSearch() {
        return new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextChange(String query) {

                if (query.length() >= 3) {
                    try {
                        searchView.getSuggestionsAdapter().changeCursor(new BuscarCidadesPorNome().execute(query).get());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    }
                } else {
                    searchView.getSuggestionsAdapter().changeCursor(null);
                }

                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {

                // if user presses enter, do default search, ex:
                if (query.length() >= 3) {
                    searchView.getSuggestionsAdapter().changeCursor(null);
                    return true;
                }

                return false;
            }
        };
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if (id == R.id.action_settings) {

            Intent settingsIntent = new Intent(this, SettingsActivity.class);

            startActivity(settingsIntent);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void pesquisarPrevisaoTempo(String localidade, String metrica) {

        listaPrevisaoTempo = new ArrayList<>();
        String[] previsoesTempo = this.buscarDadosMeteorologicos(localidade, metrica);
        if(previsoesTempo == null){
            Toast.makeText(this, R.string.error_search, Toast.LENGTH_LONG).show();;
        }else{
            for (String p : previsoesTempo) {
                listaPrevisaoTempo.add(p);
            }
        }
        arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listaPrevisaoTempo) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                // Get the Item from ListView
                View view = super.getView(position, convertView, parent);

                // Initialize a TextView for ListView each Item
                TextView tv = (TextView) view.findViewById(android.R.id.text1);

                // Set the text color of TextView (ListView Item)
                tv.setTextColor(Color.WHITE);

                // Generate ListView Item using TextView
                return view;
            }
        };

        // Set The Adapter
        listViewPrevisaoTempo.setAdapter(arrayAdapter);
    }
    /*
    private void createListDates() {

        listaPrevisaoTempo = new ArrayList<String>();

        DateTime dt = new DateTime();
        DateTimeFormatter formatterLocale = DateTimeFormat.forPattern("EEEE"); // use 'E' for short abbreviation (Mon, Tues, etc)
        String diaDaSemana = formatterLocale.withLocale(myLocale).print(dt);
        DecimalFormat df = new DecimalFormat("00");
        int diaDoMes = dt.getDayOfMonth();
        String mesDoAno = df.format(dt.getMonthOfYear());
        listaPrevisaoTempo.add(diaDaSemana + " - " + dt.getDayOfMonth() + "/" + df.format(dt.getMonthOfYear()));
        Log.i(TAG, diaDaSemana + " - " + diaDoMes + "/" + mesDoAno);
        for (int i = 1; i < 7; i++) {
            dt = dt.plusDays(1); //Adiciona um dia
            String proximoDia = formatterLocale.withLocale(myLocale).print(dt);
            listaPrevisaoTempo.add(proximoDia + " - " + dt.getDayOfMonth() + "/" + df.format(dt.getMonthOfYear()));
            Log.i(TAG, proximoDia + " - " + dt.getDayOfMonth() + "/" + df.format(dt.getMonthOfYear()));
        }

    }*/

    private String[] buscarDadosMeteorologicos(String localidade, String metrica) {
        BuscarDadosMeteorologicos buscarDadosMeteorologicos = new BuscarDadosMeteorologicos();
        try {
            return buscarDadosMeteorologicos.execute(localidade, metrica).get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void obterPreferenciasUsuario() {
        //Preferencias selecionadas pelo usuário
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        //Localidade - Nome da cidade
        localidade = settings.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        if (localidade == null || localidade.equals("")) {
            //Obtém a localização deafult
            localidade = getString(R.string.pref_location_default);
        }
        //Métrica Celsius ou Fahrenheit
        metrica = settings.getString(getString(R.string.pref_location_key), getString(R.string.pref_metric_default));
        if (metrica == null || metrica.equals("")) {
            //Obtém a métrica deafult
            metrica = getString(R.string.pref_metric_default);
        }
    }

}
