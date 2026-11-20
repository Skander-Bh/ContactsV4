package com.example.contacts.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.contacts.R
import com.example.contacts.data.ContactValidator
import com.example.contacts.databinding.FragmentCarteContactBinding
import com.example.contacts.util.IntentActions
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Locale

/**
 * Position du contact sur une carte (osmdroid) avec un marqueur. Aucune cle API n'est necessaire.
 * Plusieurs sources de tuiles gratuites sont proposees (liste deroulante) car un fournisseur peut
 * refuser les requetes ; le bouton "Ouvrir dans le navigateur" sert de solution de secours.
 * Un message explicite s'affiche si la position est absente ou invalide.
 */
class CarteContactFragment : Fragment(R.layout.fragment_carte_contact) {

    private var _binding: FragmentCarteContactBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ContactViewModel by activityViewModels()

    /** Meme ordre que le tableau carte_sources de res/values/arrays.xml. */
    private val sources: List<OnlineTileSourceBase> = listOf(
        EsriTileSource("EsriStreet", "World_Street_Map", "Source : Esri, HERE, Garmin, © OpenStreetMap contributors"),
        EsriTileSource("EsriImagery", "World_Imagery", "Source : Esri, Maxar, Earthstar Geographics"),
        XYTileSource(
            "OpenTopoMap", 0, 17, 256, ".png",
            arrayOf("https://a.tile.opentopomap.org/", "https://b.tile.opentopomap.org/", "https://c.tile.opentopomap.org/"),
            "© OpenTopoMap (CC-BY-SA), © OpenStreetMap contributors"
        ),
        XYTileSource(
            "Wikimedia", 1, 19, 256, ".png",
            arrayOf("https://maps.wikimedia.org/osm-intl/"),
            "Wikimedia maps | Données © OpenStreetMap contributors"
        ),
        XYTileSource(
            "OpenStreetMap", 0, 19, 256, ".png",
            arrayOf("https://tile.openstreetmap.org/"),
            "© OpenStreetMap contributors"
        )
    )

    private var position: GeoPoint? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureOsmdroid(requireContext().applicationContext)   // avant la creation de la MapView
    }

    /** Cache des tuiles dans le stockage interne : aucune permission de stockage requise. */
    private fun configureOsmdroid(ctx: Context) {
        val conf = Configuration.getInstance()
        conf.load(ctx, ctx.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        conf.userAgentValue = "ContactsV4-TP-Android/4.0 (projet etudiant)"
        val base = File(ctx.cacheDir, "osm_tuiles_v5")   // nouveau dossier : ignore d'anciennes tuiles d'erreur
        conf.osmdroidBasePath = base
        conf.osmdroidTileCache = File(base, "tiles")
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCarteContactBinding.bind(view)

        binding.map.setMultiTouchControls(true)

        // Liste deroulante des sources (la selection est restauree apres une rotation)
        binding.spinnerSource.adapter = ArrayAdapter(
            requireContext(), android.R.layout.simple_spinner_dropdown_item,
            resources.getStringArray(R.array.carte_sources)
        )
        binding.spinnerSource.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                applySource(sources[pos])
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        applySource(sources[0])

        binding.btnNavigateur.setOnClickListener {
            position?.let {
                val lat = String.format(Locale.US, "%.6f", it.latitude)
                val lon = String.format(Locale.US, "%.6f", it.longitude)
                IntentActions.openUrl(
                    requireContext(),
                    "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=15/$lat/$lon"
                )
            }
        }

        val contactId = requireArguments().getLong(ARG_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            val contact = viewModel.getContact(contactId)
            val lat = contact?.latitude
            val lon = contact?.longitude
            when {
                contact == null -> showMessage(R.string.err_contact_introuvable)
                lat == null || lon == null -> showMessage(R.string.carte_pas_de_position)
                !ContactValidator.isValidLatitude(lat) || !ContactValidator.isValidLongitude(lon) ->
                    showMessage(R.string.carte_position_invalide)
                else -> {
                    val point = GeoPoint(lat, lon)
                    position = point
                    binding.btnNavigateur.visibility = View.VISIBLE
                    binding.map.controller.setZoom(15.0)
                    binding.map.controller.setCenter(point)
                    val marker = Marker(binding.map).apply {
                        this.position = point
                        title = contact.nomComplet
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    }
                    binding.map.overlays.add(marker)
                    binding.map.invalidate()
                }
            }
        }
    }

    private fun applySource(source: OnlineTileSourceBase) {
        binding.map.tileProvider.clearTileCache()     // vide les tuiles de la source precedente
        binding.map.setTileSource(source)
        binding.tvAttribution.text = source.copyrightNotice
        binding.map.invalidate()
    }

    private fun showMessage(resId: Int) {
        binding.mapContainer.visibility = View.GONE
        binding.tvMessageCarte.visibility = View.VISIBLE
        binding.tvMessageCarte.setText(resId)
    }

    override fun onResume() {
        super.onResume()
        requireActivity().setTitle(R.string.titre_carte)
        _binding?.map?.onResume()
    }

    override fun onPause() {
        _binding?.map?.onPause()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding?.map?.onDetach()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val ARG_ID = "contact_id"

        fun newInstance(contactId: Long) = CarteContactFragment().apply {
            arguments = bundleOf(ARG_ID to contactId)
        }
    }
}

/**
 * Tuiles Esri : l'URL est .../tile/{z}/{y}/{x} (y avant x), d'ou la redefinition de getTileURLString.
 */
private class EsriTileSource(name: String, service: String, copyright: String) : OnlineTileSourceBase(
    name, 0, 19, 256, ".png",
    arrayOf("https://server.arcgisonline.com/ArcGIS/rest/services/$service/MapServer/tile/"),
    copyright
) {
    override fun getTileURLString(pMapTileIndex: Long): String =
        baseUrl + MapTileIndex.getZoom(pMapTileIndex) + "/" +
            MapTileIndex.getY(pMapTileIndex) + "/" + MapTileIndex.getX(pMapTileIndex)
}
